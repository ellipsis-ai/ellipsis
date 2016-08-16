define(function(require) {
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
  EnvVariableAdder = require('./env_variable_adder'),
  EnvVariableSetter = require('./env_variable_setter'),
  HelpButton = require('./help_button'),
  HiddenJsonInput = require('./hidden_json_input'),
  Notification = require('../notification'),
  SectionHeading = require('./section_heading'),
  TriggerHelp = require('./trigger_help'),
  TriggerOptionsHelp = require('./trigger_options_help'),
  TriggerInput = require('./trigger_input'),
  VersionsPanel = require('./versions_panel'),
  SVGSettingsIcon = require('../svg/settings'),
  Collapsible = require('../collapsible'),
  CsrfTokenHiddenInput = require('../csrf_token_hidden_input'),
  BrowserUtils = require('../browser_utils'),
  ImmutableObjectUtils = require('../immutable_object_utils');
  require('codemirror/mode/markdown/markdown');
  require('es6-promise');
  require('whatwg-fetch');

var AWSEnvVariableStrings = {
  accessKeyName: "AWS Access Key",
  secretKeyName: "AWS Secret Key",
  regionName: "AWS Region"
};

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
      question: React.PropTypes.string.isRequired
    })),
    triggers: React.PropTypes.arrayOf(React.PropTypes.shape({
      text: React.PropTypes.string.isRequired,
      requiresMention: React.PropTypes.bool,
      isRegex: React.PropTypes.bool,
      caseSensitive: React.PropTypes.bool
    })),
    config: React.PropTypes.shape({
      aws: React.PropTypes.shape({
        accessKeyName: React.PropTypes.string,
        secretKeyName: React.PropTypes.string,
        regionName: React.PropTypes.string
      }),
      requiredOAuth2Applications: React.PropTypes.arrayOf(
        React.PropTypes.shape({
          applicationId: React.PropTypes.string,
          displayName: React.PropTypes.string,
          parameterName: React.PropTypes.string
        })
      )
    }),
    knownEnvVarsUsed: React.PropTypes.arrayOf(React.PropTypes.string),
    csrfToken: React.PropTypes.string.isRequired,
    justSaved: React.PropTypes.bool,
    envVariables: React.PropTypes.arrayOf(React.PropTypes.object),
    oauth2Applications: React.PropTypes.arrayOf(React.PropTypes.shape({
        applicationId: React.PropTypes.string,
        displayName: React.PropTypes.string
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

  getRequiredOAuth2Applications: function() {
    return this.getBehaviorConfig()['requiredOAuth2Applications'] || [];
  },

  getAWSConfig: function() {
    return this.getBehaviorConfig()['aws'];
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
    return this.getBehaviorProp('functionBody') || "";
  },

  getBehaviorParams: function() {
    return this.getBehaviorProp('params') || [];
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
    return this.getBehaviorProp('triggers');
  },

  getBehaviorConfig: function() {
    return this.getBehaviorProp('config');
  },

  getCodeAutocompletions: function() {
    var envVars = this.getEnvVariableNames().map(function(name) {
      return 'ellipsis.env.' + name;
    });

    return this.getCodeFunctionParams().concat(envVars);
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
    var numUserParams = this.getBehaviorParams().length;
    return this.hasUserParameters() ? numUserParams + 4 : 2;
  },

  getManageDropdownLabel: function() {
    return (
      <span>
        <span className="mobile-display-none">Manage behavior</span>
        <span className="mobile-display-only">Manage</span>
      </span>
    );
  },

  buildEnvVarNotifications: function() {
    var envVars = (this.state ? this.state.envVariables : this.props.envVariables) || [];
    return envVars.
      filter(function(ea) { return this.props.knownEnvVarsUsed.includes(ea.name); }.bind(this)).
      filter(function(ea) { return !ea.isAlreadySavedWithValue; }).
      map(function(ea) {
        return {
          kind: "env_var_not_defined",
          environmentVariableName: ea.name
        };
      });
  },

  buildNotifications: function() {
    var serverNotifications = this.props.notifications || [];
    var allNotifications = serverNotifications.concat(this.buildEnvVarNotifications());

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
      return [this.getNewBlankTrigger()];
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

  getNewBlankTrigger: function() {
    return {
      text: "",
      requiresMention: false,
      isRegex: false,
      caseSensitive: false
    };
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

  getResponseHeader: function() {
    return this.state.revealCodeEditor ? "Then respond with" : "Ellipsis will respond with";
  },

  getSuccessResultTemplateHelp: function() {
    return (
      <Checklist.Item checkedWhen={this.templateIncludesSuccessResult()}>
        The result provided to <code>onSuccess</code>:<br />
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
          <Checklist className="mtxs" disabledWhen={this.isExistingBehavior()}>
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

  addParam: function() {
    var newParamIndex = this.getBehaviorParams().length + 1;
    while (this.getBehaviorParams().some(function(param) {
      return param.name === 'userInput' + newParamIndex;
    })) {
      newParamIndex++;
    }
    var newParams = this.getBehaviorParams().concat([{ name: 'userInput' + newParamIndex, question: '' }]);
    this.setBehaviorProp('params', newParams, this.focusOnLastParam);
  },

  addTrigger: function() {
    this.setBehaviorProp('triggers', this.getBehaviorTriggers().concat(this.getNewBlankTrigger()), this.focusOnFirstBlankTrigger);
  },

  cancelEnvVariableAdder: function() {
    var withoutBlanks = this.state.envVariables.filter(function(ea) { return !!ea.name; });
    this.setState({
      envVariables: withoutBlanks
    });
    this.hideActivePanel();
  },

  cancelEnvVariableSetter: function() {
    var withoutBlanks = this.state.envVariables.filter(function(ea) { return !!ea.name; });
    this.setState({
      envVariables: withoutBlanks
    });
    this.hideActivePanel();
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
    var url = jsRoutes.controllers.ApplicationController.versionInfoFor(this.props.behaviorId).url;
    this.setState({
      versionsLoadStatus: 'loading'
    });
    fetch(url, { credentials: 'same-origin' })
      .then(function(response) {
        return response.json();
      }).then(function(json) {
        this.setState({
          versions: this.state.versions.concat(json),
          versionsLoadStatus: 'loaded'
        });
        this.refs.versionsPanel.reset();
      }.bind(this)).catch(function() {
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

  onNotificationClick: function(notificationDetail) {
    if (notificationDetail && notificationDetail.kind === 'env_var_not_defined') {
      this.showEnvVariableSetter(notificationDetail);
    }
  },

  onSaveClick: function(event) {
    if (this.getBehaviorTemplate() === this.getDefaultBehaviorTemplate()) {
      event.preventDefault();
      this.setBehaviorProp('responseTemplate', this.getBehaviorTemplate(), function() {
        this.refs.behaviorForm.submit();
      }.bind(this));
    }
    this.setState({
      isSaving: true
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
    var newBehavior = ImmutableObjectUtils.objectWithNewValueAtKey(this.state.behavior, value, key);
    var timestampedBehavior = this.getTimestampedBehavior(newBehavior);
    var newVersions = ImmutableObjectUtils.arrayWithNewElementAtIndex(this.state.versions, timestampedBehavior, 0);
    if (this.state.justSaved) {
      BrowserUtils.removeQueryParam('justSaved');
    }
    this.setState({
      behavior: newBehavior,
      versions: newVersions,
      justSaved: false
    }, callback);
  },

  setConfigProperty: function(property, value) {
    var config = Object.assign({}, this.getBehaviorConfig());
    config[property] = value;
    this.setBehaviorProp('config', config);
  },

  setEnvVariableNameAtIndex: function(name, index) {
    var prevEnvVarAtIndex = this.state.envVariables[index] || {};
    var envVarAtIndex = Object.assign(prevEnvVarAtIndex, { name: name });
    this.setState({
      envVariables: ImmutableObjectUtils.arrayWithNewElementAtIndex(this.state.envVariables, envVarAtIndex, index)
    });
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

  showEnvVariableSetter: function(detailOrIndex) {
    this.toggleActivePanel('envVariableSetter', true, function() {
      if (detailOrIndex.environmentVariableName) {
        this.refs.envVariableSetterPanel.focusOnVarName(detailOrIndex.environmentVariableName);
      } else if (typeof(detailOrIndex) === 'number') {
        this.refs.envVariableSetterPanel.focusOnVarIndex(detailOrIndex);
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
    if (this.getAWSConfig()) {
      this.onRemoveAWSConfig();
    } else {
      this.setConfigProperty('aws', {});
    }
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
    });
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

  toggleTriggerOptionsHelp: function() {
    this.toggleActivePanel('helpForTriggerOptions');
  },

  toggleVersionListMenu: function() {
    this.toggleActiveDropdown('versionList');
  },

  updateCode: function(newCode) {
    this.setBehaviorProp('functionBody', newCode);
  },

  addEnvVar: function(envVar) {
    var newEnvVars = this.getEnvVariables().concat(envVar);
    var cb = function() {
      if (this.state.onNextNewEnvVar) {
        this.state.onNextNewEnvVar(envVar);
      }
    }.bind(this);
    this.updateEnvVariables(newEnvVars, cb);
  },

  updateEnvVariables: function(envVars, cb) {
    var url = jsRoutes.controllers.ApplicationController.submitEnvironmentVariables().url;
    var data = {
      teamId: this.props.teamId,
      variables: envVars
    };
    fetch(url, {
      credentials: 'same-origin',
      method: 'POST',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ teamId: this.props.teamId, dataJson: JSON.stringify(data) })
    })
      .then(function(response) {
        return response.json();
      }).then(function(json) {
        this.hideActivePanel();
        this.refs.envVariableAdderPanel.reset();
        this.setState({
          envVariables: json.variables
        }, function() {
          this.resetNotifications();
          if (cb) {
            cb();
          }
        });
      }.bind(this)).catch(function() {
        // TODO: figure out what to do if there's a request error
      });
  },

  updateParamAtIndexWithParam: function(index, newParam) {
    this.setBehaviorProp('params', ImmutableObjectUtils.arrayWithNewElementAtIndex(this.getBehaviorParams(), newParam, index));
  },

  updateTemplate: function(newTemplateString) {
    var callback = function() {
      this.setState({ hasModifiedTemplate: true });
    };
    this.setBehaviorProp('responseTemplate', newTemplateString, callback);
  },

  updateTriggerAtIndexWithTrigger: function(index, newTrigger) {
    this.setBehaviorProp('triggers', ImmutableObjectUtils.arrayWithNewElementAtIndex(this.getBehaviorTriggers(), newTrigger, index));
  },

  undoChanges: function() {
    var newBehavior = this.getInitialState().behavior;
    var timestampedBehavior = this.getTimestampedBehavior(newBehavior);
    var newVersions = ImmutableObjectUtils.arrayWithNewElementAtIndex(this.state.versions, timestampedBehavior, 0);
    this.setState({
      behavior: newBehavior,
      versions: newVersions,
      revealCodeEditor: this.shouldRevealCodeEditor()
    });
    this.hideActivePanel();
  },


  /* Booleans */

  hasCalledOnSuccess: function() {
    var code = this.getBehaviorFunctionBody();
    var success = code && code.match(/\bonSuccess\([\s\S]+?\)/);
    return !!success;
  },

  hasCode: function() {
    return !!this.getBehaviorFunctionBody().match(/\S/);
  },

  hasModalPanel: function() {
    return !!(this.state.activePanel && this.state.activePanel.modal);
  },

  hasModifiedTemplate: function() {
    return this.state.hasModifiedTemplate;
  },

  hasMultipleTriggers: function() {
    return this.getBehaviorTriggers().length > 1;
  },

  hasPrimaryTrigger: function() {
    var triggers = this.getBehaviorTriggers();
    return !!(triggers.length > 0 && triggers[0].text);
  },

  hasUserParameters: function() {
    return this.getBehaviorParams() && this.getBehaviorParams().length > 0;
  },

  isExistingBehavior: function() {
    return !!(this.props.functionBody || this.props.responseTemplate);
  },

  isModified: function() {
    var currentMatchesInitial = JSON.stringify(this.state.behavior) === JSON.stringify(this.getInitialState().behavior);
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

  triggersUseParams: function() {
    return this.getBehaviorTriggers().some(function(trigger) {
      return trigger.text.match(/{.+}/);
    });
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

  focusOnFirstBlankTrigger: function() {
    var blankTrigger = Object.keys(this.refs).find(function(key) {
      return key.match(/^trigger\d+$/) && this.refs[key].isEmpty();
    }, this);
    if (blankTrigger) {
      this.refs[blankTrigger].focus();
    }
  },

  focusOnParamIndex: function(index) {
    this.refs.codeHeader.focusIndex(index);
  },

  focusOnLastParam: function() {
    this.focusOnParamIndex(this.getBehaviorParams().length - 1);
  },

  focusOnTriggerIndex: function(index) {
    this.refs['trigger' + index].focus();
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
    var existing = this.getRequiredOAuth2Applications();
    this.setConfigProperty('requiredOAuth2Applications', existing.concat([appToAdd]));
  },

  onRemoveOAuth2Application: function(appToRemove) {
    var existing = this.getRequiredOAuth2Applications();
    this.setConfigProperty('requiredOAuth2Applications', existing.filter(function(app) {
      return app.applicationId !== appToRemove.applicationId;
    }));
  },

  onRemoveAWSConfig: function() {
    this.setConfigProperty('aws', undefined);
  },

  onParamEnterKey: function(index) {
    if (index + 1 < this.getBehaviorParams().length) {
      this.focusOnParamIndex(index + 1);
    } else if (this.getBehaviorParams()[index].question) {
      this.addParam();
    }
  },

  onTriggerEnterKey: function(index) {
    if (index + 1 < this.getBehaviorTriggers().length) {
      this.focusOnTriggerIndex(index + 1);
    } else if (this.getBehaviorTriggers()[index].text) {
      this.addTrigger();
    }
  },

  resetNotifications: function() {
    this.setState({
      notifications: this.buildNotifications()
    });
  },

    /* Component API methods */
  componentDidMount: function() {
    window.document.addEventListener('click', this.onDocumentClick, false);
    window.document.addEventListener('keydown', this.onDocumentKeyDown, false);
    window.document.addEventListener('focus', this.handleModalFocus, true);
  },

  getInitialState: function() {
    var initialBehavior = {
      teamId: this.props.teamId,
      behaviorId: this.props.behaviorId,
      functionBody: this.props.functionBody,
      responseTemplate: this.props.responseTemplate,
      params: this.props.params,
      triggers: this.getInitialTriggers(),
      config: this.props.config,
      knownEnvVarsUsed: this.props.knownEnvVarsUsed
    };
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
      envVariableAdderPrompt: null
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

      <form action="/save_behavior" method="POST" ref="behaviorForm">

        <CsrfTokenHiddenInput
          value={this.props.csrfToken}
          />
        <HiddenJsonInput
          value={JSON.stringify(this.state.behavior)}
        />

        {/* Start of container */}
        <div className="container ptxl pbxxxl">

          <div className="columns">
            <div className="column column-one-quarter mobile-column-full mts mbxxl mobile-mbs">
              <SectionHeading>When someone says</SectionHeading>

              <Checklist disabledWhen={this.isExistingBehavior()}>
                <Checklist.Item checkedWhen={this.hasPrimaryTrigger()} hiddenWhen={this.isExistingBehavior()}>
                  Write a question or phrase people should use to trigger a response.
                </Checklist.Item>
                <Checklist.Item checkedWhen={this.hasMultipleTriggers()} hiddenWhen={this.isExistingBehavior() && this.hasMultipleTriggers()}>
                  You can add multiple triggers.
                </Checklist.Item>
                <Checklist.Item checkedWhen={this.triggersUseParams()}>
                  <span>A trigger can include “fill-in-the-blank” parts, e.g. <code className="plxs">{"Call me {name}"}</code></span>
                  <span className="pls">
                    <HelpButton onClick={this.toggleTriggerHelp} toggled={this.getActivePanel() === 'helpForTriggerParameters'} />
                  </span>
                </Checklist.Item>
              </Checklist>

            </div>
            <div className="column column-three-quarters mobile-column-full pll mobile-pln mbxxl">
              <div className="mbm">
              {this.getBehaviorTriggers().map(function(trigger, index) {
                return (
                  <TriggerInput
                    className={index === 0 ? "form-input-large" : ""}
                    includeHelp={index === 0}
                    key={"BehaviorEditorTrigger" + index}
                    id={"trigger" + index}
                    ref={"trigger" + index}
                    value={trigger.text}
                    requiresMention={trigger.requiresMention}
                    isRegex={trigger.isRegex}
                    caseSensitive={trigger.caseSensitive}
                    hideDelete={!this.hasMultipleTriggers()}
                    onChange={this.updateTriggerAtIndexWithTrigger.bind(this, index)}
                    onDelete={this.deleteTriggerAtIndex.bind(this, index)}
                    onEnterKey={this.onTriggerEnterKey.bind(this, index)}
                    onHelpClick={this.toggleTriggerOptionsHelp}
                    helpVisible={this.getActivePanel() === 'helpForTriggerOptions'}
                  />
                );
              }, this)}
              </div>
              <div className="prsymbol mobile-prn align-r mobile-align-l">
                <button type="button" className="button-s" onClick={this.addTrigger}>Add another trigger</button>
              </div>
            </div>
          </div>

          <Collapsible revealWhen={this.state.revealCodeEditor}>
            <hr className="mtn" />
          </Collapsible>

          <Collapsible revealWhen={!this.state.revealCodeEditor}>
            <div className="box-help border mbxxl">
            <div className="columns columns-elastic mobile-columns-float">
              <div className="column column-expand">
                <p className="mbn">
                  <span>You can run code to determine a result, with additional input from the user if needed, </span>
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

              <SectionHeading>Ellipsis will do</SectionHeading>

              <Checklist disabledWhen={this.isExistingBehavior()}>
                <Checklist.Item checkedWhen={this.hasCode()} hiddenWhen={this.isExistingBehavior()}>
                  Write a node.js function to determine a result.
                </Checklist.Item>

                <Checklist.Item checkedWhen={this.hasCalledOnSuccess()} hiddenWhen={this.isExistingBehavior()}>
                  <span>Call <code>onSuccess</code> with a string, object, or array </span>
                  <span>to include in the response below.</span>
                </Checklist.Item>

                <Checklist.Item checkedWhen={this.hasUserParameters()} hiddenWhen={this.isExistingBehavior() && this.hasUserParameters()}>
                  <span>If you need more information from the user, add one or more parameters </span>
                  <span>to your function.</span>
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
                      requiredOAuth2Applications={this.getRequiredOAuth2Applications()}
                      onAddOAuth2Application={this.onAddOAuth2Application}
                      onRemoveOAuth2Application={this.onRemoveOAuth2Application}
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
                        onRemoveAWSConfig={this.onRemoveAWSConfig}
                        onToggleHelp={this.toggleAWSHelp}
                        helpVisible={this.getActivePanel() === 'helpForAWS'}
                      />
                    </div>
                  </Collapsible>
                </div>

                <CodeHeader
                  ref="codeHeader"
                  shouldExpandParams={this.hasUserParameters()}
                  onParamChange={this.updateParamAtIndexWithParam}
                  onParamDelete={this.deleteParamAtIndex}
                  onParamAdd={this.addParam}
                  onEnterKey={this.onParamEnterKey}
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
                <div className="position-absolute position-top-right">
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

              <SectionHeading>{this.getResponseHeader()}</SectionHeading>

              <Checklist disabledWhen={this.isExistingBehavior()}>
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

          <Collapsible revealWhen={this.getActivePanel() === 'helpForTriggerOptions'}>
            <TriggerOptionsHelp onCollapseClick={this.toggleTriggerOptionsHelp} />
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
            <EnvVariableSetter
              ref="envVariableSetterPanel"
              vars={this.getEnvVariables()}
              onCancelClick={this.cancelEnvVariableSetter}
              onChangeVarName={this.setEnvVariableNameAtIndex}
              onSave={this.updateEnvVariables}
            />
          </Collapsible>

          <Collapsible ref="envVariableAdder" revealWhen={this.getActivePanel() === 'envVariableAdder'}>
            <EnvVariableAdder
              ref="envVariableAdderPanel"
              onCancelClick={this.cancelEnvVariableAdder}
              index={this.getEnvVariables().length}
              onSave={this.addEnvVar}
              prompt={this.state.envVariableAdderPrompt}
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
                  onClick={this.onNotificationClick}
                />
              );
            }, this)}
            <div className="container ptm">
              <div className="columns columns-elastic mobile-columns-float">
                <div className="column column-expand mobile-column-auto">
                  <button type="submit"
                    className={"button-primary mrs mbm " + (this.state.isSaving ? "button-activated" : "")}
                    disabled={!this.isModified()}
                    onClick={this.onSaveClick}
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
