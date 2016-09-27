define((require) => {
var React = require('react'),
  ReactDOM = require('react-dom'),
  Codemirror = require('../react-codemirror'),
  APISelectorMenu = require('./api_selector_menu'),
  AWSConfig = require('./aws_config'),
  AWSHelp = require('./aws_help'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  BoilerplateParameterHelp = require('./boilerplate_parameter_help'),
  Checklist = require('./checklist'),
  CodeEditor = require('./code_editor'),
  CodeFooter = require('./code_footer'),
  CodeHeader = require('./code_header'),
  ConfirmActionPanel = require('./confirm_action_panel'),
  DropdownMenu = require('./dropdown_menu'),
  EnvVariableAdder = require('../environment_variables/adder'),
  EnvVariableSetter = require('../environment_variables/setter'),
  HelpButton = require('../help/help_button'),
  HiddenJsonInput = require('./hidden_json_input'),
  Notification = require('../notifications/notification'),
  SectionHeading = require('./section_heading'),
  Trigger = require('../models/trigger'),
  TriggerConfiguration = require('./trigger_configuration'),
  TriggerHelp = require('./trigger_help'),
  UserInputConfiguration = require('./user_input_configuration'),
  VersionsPanel = require('./versions_panel'),
  SVGSettingsIcon = require('../svg/settings'),
  Collapsible = require('../collapsible'),
  CsrfTokenHiddenInput = require('../csrf_token_hidden_input'),
  BrowserUtils = require('../browser_utils'),
  ImmutableObjectUtils = require('../immutable_object_utils'),
  debounce = require('javascript-debounce');
  require('codemirror/mode/markdown/markdown');
  require('whatwg-fetch');

var AWSEnvVariableStrings = {
  accessKeyName: "AWS Access Key",
  secretKeyName: "AWS Secret Key",
  regionName: "AWS Region"
};

var oauth2ApplicationShape = React.PropTypes.shape({
  apiId: React.PropTypes.string.isRequired,
  applicationId: React.PropTypes.string.isRequired,
  displayName: React.PropTypes.string,
  keyName: React.PropTypes.string,
  scope: React.PropTypes.string
});

return React.createClass({
  displayName: 'BehaviorEditor',
  mixins: [BehaviorEditorMixin],

  propTypes: {
    teamId: React.PropTypes.string.isRequired,
    behaviorId: React.PropTypes.string,
    functionBody: React.PropTypes.string,
    responseTemplate: React.PropTypes.string,
    params: React.PropTypes.arrayOf(React.PropTypes.shape({
      name: React.PropTypes.string.isRequired,
      paramType: React.PropTypes.shape({
        id: React.PropTypes.string.isRequired,
        name: React.PropTypes.string.isRequired
      }),
      question: React.PropTypes.string.isRequired
    })),
    triggers: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Trigger)),
    config: React.PropTypes.shape({
      aws: React.PropTypes.shape({
        accessKeyName: React.PropTypes.string,
        secretKeyName: React.PropTypes.string,
        regionName: React.PropTypes.string
      }),
      requiredOAuth2ApiConfigs: React.PropTypes.arrayOf(
        React.PropTypes.shape({
          id: React.PropTypes.string.isRequired,
          apiId: React.PropTypes.string.isRequired,
          recommendedScope: React.PropTypes.string,
          application: oauth2ApplicationShape
        })
      )
    }),
    knownEnvVarsUsed: React.PropTypes.arrayOf(React.PropTypes.string),
    csrfToken: React.PropTypes.string.isRequired,
    justSaved: React.PropTypes.bool,
    envVariables: React.PropTypes.arrayOf(React.PropTypes.object),
    paramTypes: React.PropTypes.arrayOf(
      React.PropTypes.shape({
        id: React.PropTypes.string.isRequired,
        name: React.PropTypes.string.isRequired
      })
    ).isRequired,
    oauth2Applications: React.PropTypes.arrayOf(oauth2ApplicationShape),
    oauth2Apis: React.PropTypes.arrayOf(React.PropTypes.shape({
      apiId: React.PropTypes.string.isRequired,
      name: React.PropTypes.string.isRequired
    })),
    notifications: React.PropTypes.arrayOf(React.PropTypes.object),
    shouldRevealCodeEditor: React.PropTypes.bool
  },


  /* Getters */

  getActiveDropdown: function() {
    return this.state.activeDropdown && this.state.activeDropdown.name ? this.state.activeDropdown.name : "";
  },

  getActiveModalElement: function() {
    if (this.state.activePanel && this.state.activePanel.name && this.state.activePanel.modal) {
      return ReactDOM.findDOMNode(this.refs[this.state.activePanel.name]);
    } else {
      return null;
    }
  },

  getActivePanel: function() {
    return this.state.activePanel && this.state.activePanel.name ? this.state.activePanel.name : "";
  },

  getAllOAuth2Applications: function() {
    return this.props.oauth2Applications || [];
  },

  getRequiredOAuth2ApiConfigs: function() {
    if (this.state) {
      return this.getBehaviorConfig()['requiredOAuth2ApiConfigs'] || [];
    } else if (this.props.config) {
      return this.props.config.requiredOAuth2ApiConfigs || [];
    } else {
      return [];
    }
  },

  getAWSConfig: function() {
    if (this.state) {
      return this.getBehaviorConfig()['aws'];
    } else if (this.props.config) {
      return this.props.config.aws;
    } else {
      return undefined;
    }
  },

  getAWSConfigProperty: function(property) {
    var config = this.getAWSConfig();
    if (config) {
      return config[property];
    } else {
      return "";
    }
  },

  getBehaviorFunctionBody: function() {
    if (this.state) {
      return this.getBehaviorProp('functionBody') || "";
    } else {
      return this.props.functionBody || "";
    }
  },

  getBehaviorParams: function() {
    if (this.state) {
      return this.getBehaviorProp('params') || [];
    } else {
      return this.props.params || [];
    }
  },

  getBehaviorProp: function(key) {
    return this.state.behavior[key];
  },

  getBehaviorStatusText: function() {
    if (this.state.justSaved) {
      return (<span className="type-green fade-in"> — saved successfully</span>);
    } else if (this.isModified()) {
      return (<span className="type-pink fade-in"> — unsaved changes</span>);
    } else {
      return (<span>&nbsp;</span>);
    }
  },

  getBehaviorTemplate: function() {
    var template = this.getBehaviorProp('responseTemplate');
    if (!template && !this.hasModifiedTemplate()) {
      return this.getDefaultBehaviorTemplate();
    } else {
      return template;
    }
  },

  getBehaviorTriggers: function() {
    if (this.state) {
      return this.getBehaviorProp('triggers');
    } else {
      return this.getInitialTriggers();
    }
  },

  getBehaviorConfig: function() {
    return this.getBehaviorProp('config');
  },

  getCodeAutocompletions: function() {
    var apiTokens =
      this.getRequiredOAuth2ApiConfigs().
        filter((config) => !!config.application).
        map((config) => `ellipsis.accessTokens.${config.application.keyName}`);

    var envVars = this.getEnvVariableNames().map(function(name) {
      return `ellipsis.env.${name}`;
    });

    var aws = this.getAWSConfig() ? ['ellipsis.AWS'] : [];

    return this.getCodeFunctionParams().concat(apiTokens, aws, envVars);
  },

  getCodeEditorDropdownLabel: function() {
    return (<SVGSettingsIcon label="Editor settings" />);
  },

  getSystemParams: function() {
    return ["ellipsis"];
  },

  getCodeFunctionParams: function() {
    var userParams = this.getBehaviorParams().map(function(param) { return param.name; });
    return userParams.concat(this.getSystemParams());
  },

  getDefaultBehaviorTemplate: function() {
    if (this.hasCalledOnSuccess()) {
      return 'The answer is: {successResult}.';
    } else {
      return this.state.magic8BallResponse;
    }
  },

  getEnvVariables: function() {
    return this.state.envVariables;
  },

  getEnvVariableNames: function() {
    return this.state.envVariables.map(function(ea) {
      return ea.name;
    });
  },

  getEnvVariableAdderPromptFor: function(property) {
    var adderString = AWSEnvVariableStrings[property];
    if (adderString) {
      return "Add a new environment variable to hold a value for the " + adderString;
    } else {
      return null;
    }

  },

  getFirstLineNumberForCode: function() {
    return 2;
  },

  getManageDropdownLabel: function() {
    return (
      <span>
        <span className="mobile-display-none">Manage behavior</span>
        <span className="mobile-display-only">Manage</span>
      </span>
    );
  },

  getRedirectValue: function() {
    return this.state.redirectValue;
  },

  buildEnvVarNotifications: function() {
    var envVars = (this.state ? this.state.envVariables : this.props.envVariables) || [];
    return envVars.
      filter((ea) => this.props.knownEnvVarsUsed.includes(ea.name)).
      filter((ea) => !ea.isAlreadySavedWithValue).
      map((ea) => ({
        kind: "env_var_not_defined",
        environmentVariableName: ea.name,
        onClick: () => { this.showEnvVariableSetter(ea.name); }
      }));
  },

  getOAuth2ApiWithId: function(apiId) {
    return this.props.oauth2Apis.find(ea => ea.apiId === apiId);
  },

  getRequiredOAuth2ApiConfigsWithNoApplication: function() {
    return this.getRequiredOAuth2ApiConfigs().filter(ea => !ea.application);
  },

  buildOAuthApplicationNotifications: function() {
    var notifications = [];
    this.getRequiredOAuth2ApiConfigsWithNoApplication().forEach(ea => {
      notifications.push({
        kind: "oauth2_config_without_application",
        name: this.getOAuth2ApiWithId(ea.apiId).name,
        requiredApiConfig: ea,
        existingOAuth2Applications: this.getAllOAuth2Applications(),
        onAddOAuth2Application: this.onAddOAuth2Application,
        onNewOAuth2Application: this.onNewOAuth2Application
      });
    });
    var unusedApplications =
      this.getRequiredOAuth2ApiConfigs().
        map(ea => ea.application).
        filter(ea => ea && !this.hasUsedOAuth2Application(ea.keyName));
    unusedApplications.forEach(ea => {
      notifications.push({
        kind: "oauth2_application_unused",
        name: ea.displayName,
        code: `ellipsis.accessTokens.${ea.keyName}`
      });
    });
    if (this.getAWSConfig() && !this.hasUsedAWSObject()) {
      notifications.push({
        kind: "aws_unused",
        code: "ellipsis.AWS"
      });
    }
    return notifications;
  },

  buildParamNotifications: function() {
    var triggerParamObj = {};
    this.getBehaviorTriggers().forEach((trigger) => {
      trigger.paramNames.forEach((paramName) => {
        triggerParamObj[paramName] = true;
      });
    });
    this.getBehaviorParams().forEach((codeParam) => {
      delete triggerParamObj[codeParam.name];
    });
    return Object.keys(triggerParamObj).map((name) => ({
      kind: "param_not_in_function",
      name: name,
      onClick: () => {
        this.addParams([name]);
      }
    }));
  },

  buildNotifications: function() {
    var serverNotifications = this.props.notifications || [];
    var allNotifications = serverNotifications.concat(
      this.buildEnvVarNotifications(),
      this.buildOAuthApplicationNotifications(),
      this.buildParamNotifications()
    );

    var notifications = {};
    allNotifications.forEach(function(notification) {
      if (notifications[notification.kind]) {
        notifications[notification.kind].push(notification);
      } else {
        notifications[notification.kind] = [notification];
      }
    });
    return Object.keys(notifications).map(function(key) {
      return {
        kind: key,
        details: notifications[key]
      };
    });
  },

  getInitialTriggers: function() {
    if (this.props.triggers && this.props.triggers.length > 0) {
      return this.props.triggers;
    } else {
      return [new Trigger()];
    }
  },

  getIterationTemplateHelp: function() {
    return (
      <Checklist.Item checkedWhen={this.templateIncludesIteration()}>
        Iterating through a list:<br />
        <div className="box-code-example">
          {"{for item in successResult.items}"}<br />
          &nbsp;* {"{item}"}<br />
          {"{endfor}"}
        </div>
      </Checklist.Item>
    );
  },

  getLastLineNumberForCode: function() {
    var numLines = this.getBehaviorFunctionBody().split('\n').length;
    return this.getFirstLineNumberForCode() + numLines;
  },

  getMagic8BallResponse: function() {
    var responses = [
      "Reply hazy try again",
      "Ask again later",
      "Better not tell you now",
      "Cannot predict now",
      "Concentrate and ask again"
    ];

    var rand = Math.floor(Math.random() * responses.length);
    return "The magic 8-ball says:\n\n“" + responses[rand] + "”";
  },

  getNotifications: function() {
    return this.state.notifications;
  },

  getPathTemplateHelp: function() {
    return (
      <Checklist.Item checkedWhen={this.templateIncludesPath()}>
        Properties of the result:<br />
        <div className="box-code-example">
          Name: {"{successResult.user.name}"}
        </div>
      </Checklist.Item>
    );
  },

  getSuccessResultTemplateHelp: function() {
    return (
      <Checklist.Item checkedWhen={this.templateIncludesSuccessResult()}>
        The result provided to <code>ellipsis.success</code>:<br />
        <div className="box-code-example">
          The answer is {"{successResult}"}
        </div>
      </Checklist.Item>
    );
  },

  getTemplateDataHelp: function() {
    if (this.state.revealCodeEditor) {
      return (
        <div>
          <span>You can include data in your response.<br /></span>
          <Checklist className="mtxs" disabledWhen={this.isFinishedBehavior()}>
            {this.getUserParamTemplateHelp()}
            {this.getSuccessResultTemplateHelp()}
            {this.getPathTemplateHelp()}
            {this.getIterationTemplateHelp()}
          </Checklist>
        </div>
      );
    }
  },

  getTimestampedBehavior: function(behavior) {
    return ImmutableObjectUtils.objectWithNewValueAtKey(behavior, 'createdAt', Date.now());
  },

  getUserParamTemplateHelp: function() {
    return (
      <Checklist.Item checkedWhen={this.templateIncludesParam()}>
        User-supplied parameters:<br />
        <div className="box-code-example">
        You said {this.hasUserParameters() && this.getBehaviorParams()[0].name ?
          "{" + this.getBehaviorParams()[0].name + "}" :
          "{exampleParamName}"}
        </div>
      </Checklist.Item>
    );
  },

  getVersions: function() {
    return this.state.versions;
  },

  /* Setters/togglers */

  createNewParam: function(optionalValues) {
    return Object.assign({
      name: '',
      question: '',
      paramType: { name: 'Text' }
    }, optionalValues);
  },

  addParam: function() {
    var newParamIndex = this.getBehaviorParams().length + 1;
    while (this.getBehaviorParams().some(function(param) {
      return param.name === 'userInput' + newParamIndex;
    })) {
      newParamIndex++;
    }
    var newParams = this.getBehaviorParams().concat([this.createNewParam({ name: 'userInput' + newParamIndex })]);
    this.setBehaviorProp('params', newParams, this.focusOnLastParam);
  },

  addParams: function(newParamNames) {
    var newParams = this.getBehaviorParams().concat(newParamNames.map((name) => this.createNewParam({ name: name })));
    this.setBehaviorProp('params', newParams);
  },

  addTrigger: function(callback) {
    this.setBehaviorProp('triggers', this.getBehaviorTriggers().concat(new Trigger()), callback);
  },

  cancelVersionPanel: function() {
    this.hideActivePanel();
    this.showVersionIndex(0);
  },

  cloneBehavior: function() {
    this.refs.cloneBehaviorForm.submit();
  },

  confirmDeleteBehavior: function() {
    this.toggleActivePanel('confirmDeleteBehavior', true);
  },

  confirmDeleteCode: function() {
    this.toggleActivePanel('confirmDeleteCode', true);
  },

  confirmUndo: function() {
    this.toggleActivePanel('confirmUndo', true);
  },

  deleteBehavior: function() {
    this.refs.deleteBehaviorForm.submit();
  },

  deleteCode: function() {
    this.setBehaviorProp('params', []);
    this.setBehaviorProp('functionBody', '');
    this.toggleCodeEditor();
    this.hideActivePanel();
  },

  deleteParamAtIndex: function(index) {
    this.setBehaviorProp('params', ImmutableObjectUtils.arrayRemoveElementAtIndex(this.getBehaviorParams(), index));
  },

  deleteTriggerAtIndex: function(index) {
    var triggers = ImmutableObjectUtils.arrayRemoveElementAtIndex(this.getBehaviorTriggers(), index);
    this.setBehaviorProp('triggers', triggers);
  },

  focusOnFirstPossibleElement: function(parentElement) {
    var tabSelector = 'a[href], area[href], input:not([disabled]), button:not([disabled]), select:not([disabled]), textarea:not([disabled]), iframe, object, embed, *[tabindex], *[contenteditable]';
    var firstFocusableElement = parentElement.querySelector(tabSelector);
    if (firstFocusableElement) {
      firstFocusableElement.focus();
    }
  },

  focusOnPrimaryOrFirstPossibleElement: function(parentElement) {
    var primaryElement = parentElement.querySelector('button.button-primary');
    if (primaryElement) {
      primaryElement.focus();
    } else {
      this.focusOnFirstPossibleElement(parentElement);
    }
  },

  loadVersions: function() {
    var url = jsRoutes.controllers.BehaviorEditorController.versionInfoFor(this.props.behaviorId).url;
    this.setState({
      versionsLoadStatus: 'loading'
    });
    fetch(url, { credentials: 'same-origin' })
      .then((response) => response.json())
      .then((json) => {
        var behaviorVersions = json.map((version) => {
          return Object.assign(version, { triggers: Trigger.triggersFromJson(version.triggers) });
        });
        this.setState({
          versions: this.state.versions.concat(behaviorVersions),
          versionsLoadStatus: 'loaded'
        });
        this.refs.versionsPanel.reset();
      }).catch(() => {
        // TODO: figure out what to do if there's a request error
        this.setState({
          versionsLoadStatus: 'error'
        });
      });
  },

  handleEscKey: function() {
    if (this.getActiveDropdown()) {
      this.hideActiveDropdown();
    } else if (this.getActivePanel()) {
      this.hideActivePanel();
    }
  },

  handleModalFocus: function(event) {
    var activeModal = this.getActiveModalElement();
    if (!activeModal) {
      return;
    }
    var focusTarget = event.target;
    var possibleMatches = activeModal.getElementsByTagName(focusTarget.tagName);
    var match = Array.prototype.some.call(possibleMatches, function(element) {
      return element === focusTarget;
    });
    if (!match) {
      event.preventDefault();
      event.stopImmediatePropagation();
      this.focusOnFirstPossibleElement(activeModal);
    }
  },

  hideActiveDropdown: function() {
    this.setState({
      activeDropdown: null
    });
  },

  hideActivePanel: function() {
    this.setState({
      activePanel: null
    });
  },

  onDocumentClick: function() {
    this.hideActiveDropdown();
  },

  onDocumentKeyDown: function(event) {
    var pressedEsc = this.eventKeyPressWasEsc(event);
    if (pressedEsc) {
      this.handleEscKey(event);
    }
  },

  onSubmit: function(maybeEvent) {
    var doSubmit = () => { this.refs.behaviorForm.submit(); };
    if (maybeEvent) {
      maybeEvent.preventDefault();
    }
    this.setState({
      isSaving: true
    }, () => {
      if (this.getBehaviorTemplate() === this.getDefaultBehaviorTemplate()) {
        this.setBehaviorProp('responseTemplate', this.getBehaviorTemplate(), doSubmit);
      } else {
        doSubmit();
      }
    });
  },

  showVersionIndex: function(versionIndex, optionalCallback) {
    var version = this.getVersions()[versionIndex];
    this.setState({
      behavior: {
        teamId: this.props.teamId,
        behaviorId: this.props.behaviorId,
        functionBody: version.functionBody,
        responseTemplate: version.responseTemplate,
        params: version.params,
        triggers: version.triggers,
        config: version.config,
        knownEnvVarsUsed: version.knownEnvVarsUsed
      },
      revealCodeEditor: !!version.functionBody,
      justSaved: false
    }, optionalCallback);
  },

  restoreVersionIndex: function(versionIndex) {
    this.showVersionIndex(versionIndex, function() {
      this.refs.behaviorForm.submit();
    });
  },

  setAWSEnvVar: function(property, envVarName) {
    var awsConfig = Object.assign({}, this.getAWSConfig());
    awsConfig[property] = envVarName;
    this.setConfigProperty('aws', awsConfig);
  },

  setBehaviorProp: function(key, value, callback) {
    var newProps = {};
    newProps[key] = value;
    this.setBehaviorProps(newProps, callback);
  },

  setBehaviorProps: function(props, callback) {
    var newBehavior = Object.assign({}, this.state.behavior, props);
    var timestampedBehavior = this.getTimestampedBehavior(newBehavior);
    var newVersions = ImmutableObjectUtils.arrayWithNewElementAtIndex(this.state.versions, timestampedBehavior, 0);
    if (this.state.justSaved) {
      BrowserUtils.removeQueryParam('justSaved');
    }
    this.setState({
      behavior: newBehavior,
      versions: newVersions,
      justSaved: false
    }, () => {
      if (callback) {
        callback();
      }
      this.resetNotifications();
    });
  },

  setConfigProperty: function(property, value, callback) {
    var config = Object.assign({}, this.getBehaviorConfig());
    config[property] = value;
    this.setBehaviorProp('config', config, callback);
  },

  showEnvVariableAdder: function(prompt) {
    this.setState({
      envVariableAdderPrompt: prompt
    }, function () {
      this.toggleActivePanel('envVariableAdder', true, function () {
        var panel = this.refs.envVariableAdderPanel;
        panel.focusOnVarName();
      }.bind(this));
    });
  },

  showEnvVariableSetter: function(nameToFocus) {
    this.toggleActivePanel('envVariableSetter', true, () => {
      if (nameToFocus) {
        this.refs.envVariableSetterPanel.focusOnVarName(nameToFocus);
      }
    });
  },

  showVersions: function() {
    if (!this.versionsMaybeLoaded()) {
      this.loadVersions();
    }
    this.toggleActivePanel('versionHistory', true);
  },

  exportVersion: function() {
    window.location = '/export_behavior/' + encodeURIComponent(this.props.behaviorId);
  },

  toggleActiveDropdown: function(name) {
    var alreadyOpen = this.getActiveDropdown() === name;
    this.setState({
      activeDropdown: alreadyOpen ? null : { name: name }
    });
  },

  toggleActivePanel: function(name, beModal, optionalCallback) {
    var alreadyOpen = this.getActivePanel() === name;
    this.setState({
      activePanel: alreadyOpen ? null : { name: name, modal: !!beModal }
    }, optionalCallback || function() {
      var activeModal = this.getActiveModalElement();
      if (activeModal) {
        this.focusOnPrimaryOrFirstPossibleElement(activeModal);
      }
    });
  },

  toggleAPISelectorMenu: function() {
    this.toggleActiveDropdown('apiSelectorDropdown');
  },

  toggleAWSConfig: function() {
    this.setConfigProperty('aws', this.getAWSConfig() ? undefined : {});
  },

  toggleAWSHelp: function() {
    this.toggleActivePanel('helpForAWS');
  },

  toggleBoilerplateHelp: function() {
    this.toggleActivePanel('helpForBoilerplateParameters');
  },

  toggleCodeEditor: function() {
    this.setState({
      revealCodeEditor: !this.state.revealCodeEditor
    }, this.resetNotifications);
  },

  toggleCodeEditorLineWrapping: function() {
    this.setState({
      codeEditorUseLineWrapping: !this.state.codeEditorUseLineWrapping
    });
  },

  toggleEditorSettingsMenu: function() {
    this.toggleActiveDropdown('codeEditorSettings');
  },

  toggleEnvVariableExpansion: function() {
    this.setState({
      expandEnvVariables: !this.state.expandEnvVariables
    });
  },

  toggleManageBehaviorMenu: function() {
    this.toggleActiveDropdown('manageBehavior');
  },

  toggleTriggerHelp: function() {
    this.toggleActivePanel('helpForTriggerParameters');
  },

  toggleVersionListMenu: function() {
    this.toggleActiveDropdown('versionList');
  },

  updateCode: function(newCode) {
    this.setBehaviorProp('functionBody', newCode);
  },

  addEnvVar: function(envVar) {
    var newEnvVars = this.getEnvVariables().concat(envVar);
    this.updateEnvVariables(newEnvVars, {
      saveCallback: () => {
        if (this.state.onNextNewEnvVar) {
          this.state.onNextNewEnvVar(envVar);
        }
      },
      errorCallback: () => {
        this.refs.envVariableAdderPanel.onSaveError();
      }
    });
  },

  updateEnvVariables: function(envVars, options) {
    var url = jsRoutes.controllers.EnvironmentVariablesController.submit().url;
    var data = {
      teamId: this.props.teamId,
      variables: envVars
    };
    fetch(url, {
      credentials: 'same-origin',
      method: 'POST',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'Csrf-Token': this.props.csrfToken
      },
      body: JSON.stringify({ teamId: this.props.teamId, dataJson: JSON.stringify(data) })
    })
      .then((response) => response.json())
      .then((json) => {
        this.hideActivePanel();
        this.refs.envVariableAdderPanel.reset();
        this.setState({
          envVariables: json.variables
        }, () => {
          this.resetNotifications();
          this.refs.envVariableSetterPanel.reset();
          if (options && options.saveCallback) {
            options.saveCallback();
          }
        });
      }).catch(() => {
        this.refs.envVariableSetterPanel.onSaveError();
        if (options && options.errorCallback) {
          options.errorCallback();
        }
      });
  },

  updateParamAtIndexWithParam: function(index, newParam) {
    var oldParams = this.getBehaviorParams();
    var oldParamName = oldParams[index].name;
    var newParamName = newParam.name;
    var newParams = ImmutableObjectUtils.arrayWithNewElementAtIndex(oldParams, newParam, index);
    this.setBehaviorProp('params', newParams, () => {
      var numTriggersReplaced = 0;
      if (oldParamName === this.state.paramNameToSync) {
        numTriggersReplaced = this.syncParamNamesAndCount(oldParamName, newParamName);
        if (numTriggersReplaced > 0) {
          this.setState({ paramNameToSync: newParamName });
        }
      }
    });
  },

  updateTemplate: function(newTemplateString) {
    this.setBehaviorProp('responseTemplate', newTemplateString, () => {
      this.setState({ hasModifiedTemplate: true });
    });
  },

  syncParamNamesAndCount: function(oldName, newName) {
    var pattern = new RegExp(`\{${oldName}\}`, 'g');
    var newString = `{${newName}}`;
    var numTriggersModified = 0;

    var newTriggers = this.getBehaviorTriggers().map((oldTrigger) => {
      if (oldTrigger.usesParamName(oldName)) {
        numTriggersModified++;
        return oldTrigger.clone({ text: oldTrigger.getTextWithNewParamName(oldName, newName) });
      } else {
        return oldTrigger;
      }
    });

    var oldTemplate = this.getBehaviorTemplate();
    var newTemplate = oldTemplate.replace(pattern, newString);
    var templateModified = newTemplate !== oldTemplate;

    var newProps = {};
    if (numTriggersModified > 0) {
      newProps.triggers = newTriggers;
    }
    if (templateModified) {
      newProps.responseTemplate = newTemplate;
    }
    if (Object.keys(newProps).length > 0) {
      this.setBehaviorProps(newProps);
    }

    return numTriggersModified + (templateModified ? 1 : 0);
  },

  updateTriggerAtIndexWithTrigger: function(index, newTrigger) {
    this.setBehaviorProp('triggers', ImmutableObjectUtils.arrayWithNewElementAtIndex(this.getBehaviorTriggers(), newTrigger, index));
  },

  undoChanges: function() {
    var newBehavior = this.getInitialBehavior();
    var timestampedBehavior = this.getTimestampedBehavior(newBehavior);
    var newVersions = ImmutableObjectUtils.arrayWithNewElementAtIndex(this.state.versions, timestampedBehavior, 0);
    this.setState({
      behavior: newBehavior,
      versions: newVersions,
      revealCodeEditor: this.shouldRevealCodeEditor()
    }, () => {
      this.hideActivePanel();
      this.resetNotifications();
    });
  },


  /* Booleans */

  hasCalledOnError: function() {
    var code = this.getBehaviorFunctionBody();
    return /\bonError\(\s*\S.+?\)/.test(code) ||
      /\bellipsis\.error\(\s*\S.+?\)/.test(code);
  },

  hasCalledNoResponse: function() {
    return /\bellipsis\.noResponse\(\)/.test(this.getBehaviorFunctionBody());
  },

  hasCalledOnSuccess: function() {
    var code = this.getBehaviorFunctionBody();
    return /\bonSuccess\([\s\S]*?\)/.test(code) ||
      /\bellipsis\.success\([\s\S]*?\)/.test(code);
  },

  hasCalledRequire: function() {
    var code = this.getBehaviorFunctionBody();
    return /\brequire\(\s*\S.+?\)/.test(code);
  },

  hasCode: function() {
    return /\S/.test(this.getBehaviorFunctionBody());
  },

  hasUsedAWSObject: function() {
    var code = this.getBehaviorFunctionBody();
    return /\bellipsis\.AWS\b/.test(code);
  },

  hasUsedOAuth2Application: function(keyName) {
    var code = this.getBehaviorFunctionBody();
    var pattern = new RegExp(`\\bellipsis\\.accessTokens\\.${keyName}\\b`);
    return pattern.test(code);
  },

  hasModalPanel: function() {
    return !!(this.state.activePanel && this.state.activePanel.modal);
  },

  hasModifiedTemplate: function() {
    return this.state.hasModifiedTemplate;
  },

  hasUserParameters: function() {
    return this.getBehaviorParams() && this.getBehaviorParams().length > 0;
  },

  isExistingBehavior: function() {
    return !!this.props.behaviorId;
  },

  isFinishedBehavior: function() {
    return this.isExistingBehavior() && !!(this.props.functionBody || this.props.responseTemplate);
  },

  isModified: function() {
    var currentMatchesInitial = JSON.stringify(this.state.behavior) === JSON.stringify(this.getInitialBehavior());
    var previewingVersions = this.getActivePanel() === 'versionHistory';
    return !currentMatchesInitial && !previewingVersions;
  },

  shouldFilterCurrentVersion: function() {
    var firstTwoVersions = this.getVersions().slice(0, 2);
    return firstTwoVersions.length === 2 && this.versionEqualsVersion(firstTwoVersions[0], firstTwoVersions[1]);
  },

  shouldRevealCodeEditor: function() {
    return !!(this.props.shouldRevealCodeEditor || this.props.functionBody);
  },

  templateIncludesIteration: function() {
    var template = this.getBehaviorTemplate();
    return !!(template && template.match(/\{endfor\}/));
  },

  templateUsesMarkdown: function() {
    var template = this.getBehaviorTemplate();
    /* Big ugly flaming pile of regex to try and guess at Markdown usage: */
    var matches = [
      '\\*.+?\\*', /* Bold/italics */
      '_.+?_', /* Bold/italics */
      '\\[.+?\\]\\(.+?\\)', /* Links */
      '(\\[.+?\\]){2}', /* Links by reference */
      '^.+\\n[=-]+', /* Underlined headers */
      '^#+\\s+.+', /* # Headers */
      '^\\d\\.\\s+.+', /* Numbered lists */
      '^\\*\\s+.+', /* Bulleted lists */
      '^>.+', /* Block quote */
      '`.+?`', /* Code */
      '```', /* Code block */
      '^\\s*[-\\*]\\s*[-\\*]\\s*[-\\*]' /* Horizontal rule */
    ];
    var matchRegExp = new RegExp( '(' + matches.join( ')|(' ) + ')' );
    return !!(template && template.match(matchRegExp));
  },

  templateIncludesParam: function() {
    var template = this.getBehaviorTemplate();
    return !!(template && template.match(/\{\S+?\}/));
  },

  templateIncludesPath: function() {
    var template = this.getBehaviorTemplate();
    return !!(template && template.match(/\{(\S+\.\S+)+?\}/));
  },

  templateIncludesSuccessResult: function() {
    var template = this.getBehaviorTemplate();
    return !!(template && template.match(/\{successResult.*?\}/));
  },

  versionEqualsVersion: function(version1, version2) {
    var shallow1 = JSON.stringify({
      functionBody: version1.functionBody,
      responseTemplate: version1.responseTemplate,
      params: version1.params,
      triggers: version1.triggers,
      config: version1.config
    });
    var shallow2 = JSON.stringify({
      functionBody: version2.functionBody,
      responseTemplate: version2.responseTemplate,
      params: version2.params,
      triggers: version2.triggers,
      config: version2.config
    });
    return shallow1 === shallow2;
  },

  versionsMaybeLoaded: function() {
    return this.state.versionsLoadStatus === 'loading' || this.state.versionsLoadStatus === 'loaded';
  },

  /* Interaction and event handling */

  ensureCursorVisible: function(editor) {
    if (!this.refs.footer) {
      return;
    }
    var cursorBottom = editor.cursorCoords(false).bottom;
    BrowserUtils.ensureYPosInView(cursorBottom, this.refs.footer.clientHeight);
  },

  focusOnParamIndex: function(index) {
    this.refs.userInputConfiguration.focusIndex(index);
  },

  focusOnLastParam: function() {
    this.focusOnParamIndex(this.getBehaviorParams().length - 1);
  },

  onAddNewEnvVariable: function() {
    this.showEnvVariableAdder();
  },

  onAWSAddNewEnvVariable: function(property) {
    this.setState({
      onNextNewEnvVar: function(newVar) {
        this.setAWSEnvVar(property, newVar.name);
      }.bind(this)
    }, function() {
      this.showEnvVariableAdder(this.getEnvVariableAdderPromptFor(property));
    });
  },

  onAWSConfigChange: function(property, envVarName) {
    this.setAWSEnvVar(property, envVarName);
  },

  onAddOAuth2Application: function(appToAdd) {
    var existing = this.getRequiredOAuth2ApiConfigs();
    var indexToReplace = existing.findIndex(ea => ea.apiId === appToAdd.apiId && !ea.application);
    var toReplace = existing[indexToReplace];
    var configs = existing.slice();
    if (indexToReplace >= 0) {
      configs.splice(indexToReplace, 1);
    }
    var toAdd = Object.assign({}, toReplace, {
      apiId: appToAdd.apiId,
      application: appToAdd
    });
    this.setConfigProperty('requiredOAuth2ApiConfigs', configs.concat([toAdd]));
  },

  onRemoveOAuth2Application: function(appToRemove) {
    var existing = this.getRequiredOAuth2ApiConfigs();
    this.setConfigProperty('requiredOAuth2ApiConfigs', existing.filter(function(config) {
      return config.application && config.application.applicationId !== appToRemove.applicationId;
    }));
  },

  onNewOAuth2Application: function(requiredOAuth2ApiConfigId) {
    this.setState({
      redirectValue: "newOAuth2Application",
      requiredOAuth2ApiConfigId: requiredOAuth2ApiConfigId || ""
    }, () => { this.onSubmit(); });
  },

  onParamEnterKey: function(index) {
    if (index + 1 < this.getBehaviorParams().length) {
      this.focusOnParamIndex(index + 1);
    } else if (this.getBehaviorParams()[index].question) {
      this.addParam();
    }
  },

  onParamNameFocus: function(index) {
    this.setState({
      paramNameToSync: this.getBehaviorParams()[index].name
    });
  },

  onParamNameBlur: function() {
    this.setState({
      paramNameToSync: null
    });
  },

  resetNotificationsImmediately: function() {
    var newNotifications = this.buildNotifications();
    var newKinds = newNotifications.map(ea => ea.kind);
    var visibleAndUnneeded = (notification) => !notification.hidden && !newKinds.some(kind => kind === notification.kind);
    var notificationsToHide = this.getNotifications().filter(visibleAndUnneeded)
      .map(deadNotification => Object.assign(deadNotification, { hidden: true }));
    this.setState({
      notifications: newNotifications.concat(notificationsToHide)
    });
  },

  resetNotifications: debounce(function() {
    this.resetNotificationsImmediately();
  }, 250),

    /* Component API methods */
  componentDidMount: function() {
    window.document.addEventListener('click', this.onDocumentClick, false);
    window.document.addEventListener('keydown', this.onDocumentKeyDown, false);
    window.document.addEventListener('focus', this.handleModalFocus, true);
  },

  getInitialBehavior: function() {
    return {
      teamId: this.props.teamId,
      behaviorId: this.props.behaviorId,
      functionBody: this.props.functionBody,
      responseTemplate: this.props.responseTemplate,
      params: this.props.params,
      triggers: this.getInitialTriggers(),
      config: this.props.config,
      knownEnvVarsUsed: this.props.knownEnvVarsUsed
    };
  },

  getInitialState: function() {
    var initialBehavior = this.getInitialBehavior();
    return {
      behavior: initialBehavior,
      activeDropdown: null,
      activePanel: null,
      codeEditorUseLineWrapping: false,
      expandEnvVariables: false,
      justSaved: this.props.justSaved,
      isSaving: false,
      envVariables: this.props.envVariables || [],
      revealCodeEditor: this.shouldRevealCodeEditor(),
      magic8BallResponse: this.getMagic8BallResponse(),
      hasModifiedTemplate: !!this.props.responseTemplate,
      notifications: this.buildNotifications(),
      versions: [this.getTimestampedBehavior(initialBehavior)],
      versionsLoadStatus: null,
      onNextNewEnvVar: null,
      envVariableAdderPrompt: null,
      redirectValue: "",
      requiredOAuth2ApiConfigId: "",
      paramNameToSync: null
    };
  },

  render: function() {
    return (

      <div>

      <div className="bg-light">
        <div className="container pbm">
          <h3 className="mvn ptxxl type-weak display-ellipsis">
            <span>Edit behavior</span>
            <span className="type-italic">{this.getBehaviorStatusText()}</span>
          </h3>

            {/*
             <form ref="testBehaviorForm" action="/test_behavior_version" method="POST">
               <CsrfTokenHiddenInput value={this.props.csrfToken} />
               <input type="text" name="message" />
               <input type="hidden" name="behaviorId" value={this.props.behaviorId} />
               <input type="submit" />
             </form>
            */}
        </div>
      </div>

      <form action="/save_behavior" method="POST" ref="behaviorForm" onSubmit={this.onSubmit}>

        <CsrfTokenHiddenInput
          value={this.props.csrfToken}
          />
        <HiddenJsonInput
          value={JSON.stringify(this.state.behavior)}
        />
        <input type="hidden" name="redirect" value={this.getRedirectValue()} />
        <input type="hidden" name="requiredOAuth2ApiConfigId" value={this.state.requiredOAuth2ApiConfigId} />

        {/* Start of container */}
        <div className="container ptxl pbxxxl">

          <TriggerConfiguration
            isFinishedBehavior={this.isFinishedBehavior()}
            triggers={this.getBehaviorTriggers()}
            onToggleHelp={this.toggleTriggerHelp}
            helpVisible={this.getActivePanel() === 'helpForTriggerParameters'}
            onTriggerAdd={this.addTrigger}
            onTriggerChange={this.updateTriggerAtIndexWithTrigger}
            onTriggerDelete={this.deleteTriggerAtIndex}
            onTriggerDropdownToggle={this.toggleActiveDropdown}
            openDropdownName={this.getActiveDropdown()}
          />

          <UserInputConfiguration
            ref="userInputConfiguration"
            onParamChange={this.updateParamAtIndexWithParam}
            onParamDelete={this.deleteParamAtIndex}
            onParamAdd={this.addParam}
            onParamNameFocus={this.onParamNameFocus}
            onParamNameBlur={this.onParamNameBlur}
            onEnterKey={this.onParamEnterKey}
            userParams={this.getBehaviorParams()}
            paramTypes={this.props.paramTypes}
            triggers={this.getBehaviorTriggers()}
            isFinishedBehavior={this.isFinishedBehavior()}
            behaviorHasCode={this.state.revealCodeEditor}
          />

          <Collapsible revealWhen={this.state.revealCodeEditor}>
            <hr className="mtn" />
          </Collapsible>

          <Collapsible revealWhen={!this.state.revealCodeEditor}>
            <div className="box-help border mbxxl">
            <div className="columns columns-elastic mobile-columns-float">
              <div className="column column-expand">
                <p className="mbn">
                  <span>You can run code to determine a result, using any inputs you’ve specified above, </span>
                  <span>or provide a simple response below.</span>
                </p>
              </div>
              <div className="column column-shrink align-m mobile-mtm">
                <button type="button" className="button-s" onClick={this.toggleCodeEditor}>
                  Add code
                </button>
              </div>
            </div>
            </div>
          </Collapsible>

          <Collapsible revealWhen={this.state.revealCodeEditor} animationDuration={0.5}>
          <div className="columns">
            <div className="column column-one-quarter mobile-column-full mbxxl mobile-mbs">

              <SectionHeading>Then Ellipsis will do</SectionHeading>

              <Checklist disabledWhen={this.isFinishedBehavior()}>
                <Checklist.Item checkedWhen={this.hasCode()} hiddenWhen={this.isFinishedBehavior()}>
                  <span>Write a node.js function. You can <code>require()</code> any </span>
                  <span><a href="https://www.npmjs.com/" target="_blank">NPM package</a>.</span>
                </Checklist.Item>

                <Checklist.Item checkedWhen={this.hasCalledOnSuccess()} hiddenWhen={this.isFinishedBehavior()}>
                  <span>End the function by calling </span>
                  <code className="type-bold">ellipsis.success(<span className="type-regular">…</span>)</code>
                  <span> with text or data to include in the response. </span>
                  <button type="button" className="button-raw link button-s" onClick={this.toggleBoilerplateHelp}>Examples</button>
                </Checklist.Item>

                <Checklist.Item checkedWhen={this.hasCalledOnError()} hiddenWhen={this.isFinishedBehavior()}>
                  <span>To end with an error message, call </span>
                  <code className="type-bold">ellipsis.error(<span className="type-regular">…</span>)</code>
                  <span> with a string. </span>
                  <button type="button" className="button-raw link button-s" onClick={this.toggleBoilerplateHelp}>Example</button>
                </Checklist.Item>

                <Checklist.Item checkedWhen={this.hasCalledNoResponse()} hiddenWhen={this.isFinishedBehavior()}>
                  <span>To end with no response, call <code className="type-bold">ellipsis.noResponse()</code>.</span>
                </Checklist.Item>

                <Checklist.Item hiddenWhen={!this.isFinishedBehavior()}>
                  <span>Call <code>ellipsis.success(…)</code>, <code>ellipsis.error(…)</code> and/or <code>ellipsis.noResponse()</code> </span>
                  <span>to end your function. </span>
                  <span className="pls">
                    <HelpButton onClick={this.toggleBoilerplateHelp} toggled={this.getActivePanel() === 'helpForBoilerplateParameters'} />
                  </span>
                </Checklist.Item>

                <Checklist.Item checkedWhen={this.hasUserParameters()} hiddenWhen={this.isFinishedBehavior() && this.hasUserParameters()}>
                  <span>If you need more information from the user, add one or more inputs above </span>
                  <span>and the function will receive them as parameters.</span>
                </Checklist.Item>

                <Checklist.Item hiddenWhen={!this.isFinishedBehavior() || this.hasCalledRequire()}>
                  <span>Use <code>require(…)</code> to load any NPM package.</span>
                </Checklist.Item>

              </Checklist>

            </div>

            <div className="column column-three-quarters mobile-column-full pll mobile-pln mbxxl">
              <div className="border-top border-left border-right border-radius-top pts">
                <div className="ptxs type-s">
                  <div className="phm mbm">
                    <APISelectorMenu
                      openWhen={this.getActiveDropdown() === 'apiSelectorDropdown'}
                      onAWSClick={this.toggleAWSConfig}
                      awsCheckedWhen={!!this.getAWSConfig()}
                      toggle={this.toggleAPISelectorMenu}
                      allOAuth2Applications={this.getAllOAuth2Applications()}
                      requiredOAuth2ApiConfigs={this.getRequiredOAuth2ApiConfigs()}
                      onAddOAuth2Application={this.onAddOAuth2Application}
                      onRemoveOAuth2Application={this.onRemoveOAuth2Application}
                      onNewOAuth2Application={this.onNewOAuth2Application}
                      getOAuth2ApiWithId={this.getOAuth2ApiWithId}
                      />
                  </div>

                  <Collapsible revealWhen={!!this.getAWSConfig()}>
                    <div className="phm pbs mbs border-bottom">
                      <AWSConfig
                        envVariableNames={this.getEnvVariableNames()}
                        accessKeyName={this.getAWSConfigProperty('accessKeyName')}
                        secretKeyName={this.getAWSConfigProperty('secretKeyName')}
                        regionName={this.getAWSConfigProperty('regionName')}
                        onAddNew={this.onAWSAddNewEnvVariable}
                        onChange={this.onAWSConfigChange}
                        onRemoveAWSConfig={this.toggleAWSConfig}
                        onToggleHelp={this.toggleAWSHelp}
                        helpVisible={this.getActivePanel() === 'helpForAWS'}
                      />
                    </div>
                  </Collapsible>
                </div>

                <CodeHeader
                  ref="codeHeader"
                  helpVisible={this.getActivePanel() === 'helpForBoilerplateParameters'}
                  onToggleHelp={this.toggleBoilerplateHelp}
                  userParams={this.getBehaviorParams()}
                  systemParams={this.getSystemParams()}
                />
              </div>

              <div className="position-relative">
                <CodeEditor
                  value={this.getBehaviorFunctionBody()}
                  onChange={this.updateCode}
                  onCursorChange={this.ensureCursorVisible}
                  firstLineNumber={this.getFirstLineNumberForCode()}
                  lineWrapping={this.state.codeEditorUseLineWrapping}
                  autocompletions={this.getCodeAutocompletions()}
                  functionParams={this.getCodeFunctionParams()}
                />
                <div className="position-absolute position-top-right position-z-popup-trigger">
                  <DropdownMenu
                    openWhen={this.getActiveDropdown() === 'codeEditorSettings'}
                    label={this.getCodeEditorDropdownLabel()}
                    labelClassName="button-dropdown-trigger-symbol"
                    menuClassName="popup-dropdown-menu-right"
                    toggle={this.toggleEditorSettingsMenu}
                  >
                    <DropdownMenu.Item
                      onClick={this.toggleCodeEditorLineWrapping}
                      checkedWhen={this.state.codeEditorUseLineWrapping}
                      label="Enable line wrap"
                    />
                  </DropdownMenu>
                </div>
              </div>

              <CodeFooter
                lineNumber={this.getLastLineNumberForCode()}
                onCodeDelete={this.confirmDeleteCode}
              />

            </div>
          </div>

          <hr className="mtn" />
          </Collapsible>

          <div className="columns">

            <div className="column column-one-quarter mobile-column-full mbxl mobile-mbs type-s">

              <SectionHeading>Then Ellipsis will respond with</SectionHeading>

              <Checklist disabledWhen={this.isFinishedBehavior()}>
                <Checklist.Item checkedWhen={this.templateUsesMarkdown()}>
                  <span>Use <a href="http://commonmark.org/help/" target="_blank">Markdown</a> </span>
                  <span>to format the response, add links, etc.</span>
                </Checklist.Item>
                {this.state.revealCodeEditor ? "" : (
                  <Checklist.Item>Add code above if you want to collect user input before returning a response.</Checklist.Item>
                )}
              </Checklist>

              {this.getTemplateDataHelp()}
            </div>

            <div className="column column-three-quarters mobile-column-full pll mobile-pln mbxxxl">
              <div className="position-relative CodeMirror-container-no-gutter">
                <Codemirror value={this.getBehaviorTemplate()}
                  onChange={this.updateTemplate}
                  onCursorChange={this.ensureCursorVisible}
                  options={{
                    mode: {
                      name: "markdown",
                      /* Use CommonMark-appropriate settings */
                      fencedCodeBlocks: true,
                      underscoresBreakWords: false
                    },
                    gutters: ['CodeMirror-no-gutter'],
                    indentUnit: 4,
                    indentWithTabs: true,
                    lineWrapping: true,
                    lineNumbers: false,
                    smartIndent: true,
                    tabSize: 4,
                    viewportMargin: Infinity,
                    placeholder: "The result is {successResult}"
                  }}
                />
              </div>
            </div>
          </div>
        </div> {/* End of container */}

        <div className={"bg-scrim position-z-almost-front position-fixed-full " + (this.hasModalPanel() ? "fade-in" : "display-none")}></div>
        <footer ref="footer" className={"position-fixed-bottom position-z-front border-top " +
          (this.isModified() ? "bg-white" : "bg-light-translucent")}
        >
          <Collapsible ref="confirmUndo" revealWhen={this.getActivePanel() === 'confirmUndo'}>
            <ConfirmActionPanel confirmText="Undo changes" onConfirmClick={this.undoChanges} onCancelClick={this.hideActivePanel}>
              <p>This will undo any changes you’ve made since last saving. Are you sure you want to do this?</p>
            </ConfirmActionPanel>
          </Collapsible>

          <Collapsible ref="confirmDeleteBehavior" revealWhen={this.getActivePanel() === 'confirmDeleteBehavior'}>
            <ConfirmActionPanel confirmText="Delete" onConfirmClick={this.deleteBehavior} onCancelClick={this.hideActivePanel}>
              <p>Are you sure you want to delete this behavior?</p>
            </ConfirmActionPanel>
          </Collapsible>

          <Collapsible ref="confirmDeleteCode" revealWhen={this.getActivePanel() === 'confirmDeleteCode'}>
            <ConfirmActionPanel confirmText="Remove" onConfirmClick={this.deleteCode} onCancelClick={this.hideActivePanel}>
              <p>Are you sure you want to remove all of the code?</p>
            </ConfirmActionPanel>
          </Collapsible>

          <Collapsible revealWhen={this.getActivePanel() === 'helpForTriggerParameters'}>
            <TriggerHelp onCollapseClick={this.toggleTriggerHelp} />
          </Collapsible>

          <Collapsible revealWhen={this.getActivePanel() === 'helpForBoilerplateParameters'}>
            <BoilerplateParameterHelp
              envVariableNames={this.getEnvVariableNames()}
              expandEnvVariables={this.state.expandEnvVariables}
              onAddNew={this.onAddNewEnvVariable}
              onExpandToggle={this.toggleEnvVariableExpansion}
              onCollapseClick={this.toggleBoilerplateHelp}
            />
          </Collapsible>

          <Collapsible revealWhen={this.getActivePanel() === 'helpForAWS'}>
            <AWSHelp onCollapseClick={this.toggleAWSHelp} />
          </Collapsible>

          <Collapsible ref="versionHistory" revealWhen={this.getActivePanel() === 'versionHistory'}>
            <VersionsPanel
              ref="versionsPanel"
              menuToggle={this.toggleVersionListMenu}
              onCancelClick={this.cancelVersionPanel}
              onRestoreClick={this.restoreVersionIndex}
              onSwitchVersions={this.showVersionIndex}
              openMenuWhen={this.getActiveDropdown() === 'versionList'}
              shouldFilterCurrentVersion={this.shouldFilterCurrentVersion()}
              versions={this.getVersions()}
            />
          </Collapsible>

          <Collapsible ref="envVariableSetter" revealWhen={this.getActivePanel() === 'envVariableSetter'}>
            <div className="box-action">
              <div className="container phn">
                <div className="columns">
                  <div className="column column-one-quarter mobile-column-full"></div>
                  <div className="column column-three-quarters  mobile-column-full">
                    <EnvVariableSetter
                      ref="envVariableSetterPanel"
                      vars={this.getEnvVariables()}
                      onCancelClick={this.hideActivePanel}
                      onSave={this.updateEnvVariables}
                    />
                  </div>
                </div>
              </div>
            </div>
          </Collapsible>

          <Collapsible ref="envVariableAdder" revealWhen={this.getActivePanel() === 'envVariableAdder'}>
            <EnvVariableAdder
              ref="envVariableAdderPanel"
              onCancelClick={this.hideActivePanel}
              onSave={this.addEnvVar}
              prompt={this.state.envVariableAdderPrompt}
              existingNames={this.getEnvVariableNames()}
            />
          </Collapsible>

          <Collapsible revealWhen={!this.hasModalPanel()}>
            {this.getNotifications().map(function(notification, index) {
              return (
                <Notification
                  key={"notification" + index}
                  details={notification.details}
                  index={index}
                  kind={notification.kind}
                  hidden={notification.hidden}
                />
              );
            }, this)}
            <div className="container ptm">
              <div className="columns columns-elastic mobile-columns-float">
                <div className="column column-expand mobile-column-auto">
                  <button type="submit"
                    className={"button-primary mrs mbm " + (this.state.isSaving ? "button-activated" : "")}
                    disabled={!this.isModified()}
                  >
                    <span className="button-labels">
                      <span className="button-normal-label">
                        <span className="mobile-display-none">Save changes</span>
                        <span className="mobile-display-only">Save</span>
                      </span>
                      <span className="button-activated-label">Saving…</span>
                    </span>
                  </button>
                  <button className="mbm" type="button" disabled={!this.isModified()} onClick={this.confirmUndo}>
                    <span className="mobile-display-none">Undo changes</span>
                    <span className="mobile-display-only">Undo</span>
                  </button>
                </div>
                <div className="column column-shrink align-r pbm">
                  {this.isExistingBehavior() ? (
                    <DropdownMenu
                      openWhen={this.getActiveDropdown() === 'manageBehavior'}
                      label={this.getManageDropdownLabel()}
                      labelClassName="button-dropdown-trigger-menu-above"
                      menuClassName="popup-dropdown-menu-right popup-dropdown-menu-above"
                      toggle={this.toggleManageBehaviorMenu}
                    >
                      <DropdownMenu.Item onClick={this.showVersions} label="View/restore previous versions" />
                      <DropdownMenu.Item onClick={this.exportVersion} label="Export this behavior" />
                      <DropdownMenu.Item onClick={this.cloneBehavior} label="Clone this behavior" />
                      <DropdownMenu.Item onClick={this.confirmDeleteBehavior} label="Delete behavior" />
                    </DropdownMenu>
                  ) : null}
                </div>
              </div>
            </div>
          </Collapsible>

        </footer>

      </form>
      <form className="pbxxxl" ref="deleteBehaviorForm" action="/delete_behavior" method="POST">
        <CsrfTokenHiddenInput value={this.props.csrfToken} />
        <input type="hidden" name="behaviorId" value={this.props.behaviorId} />
      </form>

      <form className="pbxxxl" ref="cloneBehaviorForm" action="/clone_behavior" method="POST">
        <CsrfTokenHiddenInput value={this.props.csrfToken} />
        <input type="hidden" name="behaviorId" value={this.props.behaviorId} />
      </form>

      </div>
    );
  }
});

});
