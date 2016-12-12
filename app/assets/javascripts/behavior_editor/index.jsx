define((require) => {
var React = require('react'),
  ReactDOM = require('react-dom'),
  APISelectorMenu = require('./api_selector_menu'),
  AWSConfig = require('./aws_config'),
  AWSHelp = require('./aws_help'),
  BehaviorVersion = require('../models/behavior_version'),
  BehaviorSwitcher = require('./behavior_switcher'),
  BehaviorTester = require('./behavior_tester'),
  DataTypeTester = require('./data_type_tester'),
  BoilerplateParameterHelp = require('./boilerplate_parameter_help'),
  CodeEditor = require('./code_editor'),
  CodeEditorHelp = require('./code_editor_help'),
  CodeFooter = require('./code_footer'),
  CodeHeader = require('./code_header'),
  ConfirmActionPanel = require('./confirm_action_panel'),
  DataTypeCodeEditorHelp = require('./data_type_code_editor_help'),
  DataTypeNameInput = require('./data_type_name_input'),
  DataTypeResultConfig = require('./data_type_result_config'),
  DynamicLabelButton = require('../form/dynamic_label_button'),
  DropdownMenu = require('../dropdown_menu'),
  EnvVariableAdder = require('../environment_variables/adder'),
  EnvVariableSetter = require('../environment_variables/setter'),
  FixedFooter = require('../fixed_footer'),
  HiddenJsonInput = require('./hidden_json_input'),
  Input = require('../form/input'),
  ModalScrim = require('./modal_scrim'),
  Notification = require('../notifications/notification'),
  Param = require('../models/param'),
  ResponseTemplate = require('../models/response_template'),
  ResponseTemplateConfiguration = require('./response_template_configuration'),
  SectionHeading = require('./section_heading'),
  SharedAnswerInputSelector = require('./shared_answer_input_selector'),
  SVGHamburger = require('../svg/hamburger'),
  Trigger = require('../models/trigger'),
  TriggerConfiguration = require('./trigger_configuration'),
  TriggerHelp = require('./trigger_help'),
  UniqueBy = require('../unique_by'),
  UserInputConfiguration = require('./user_input_configuration'),
  VersionsPanel = require('./versions_panel'),
  SVGSettingsIcon = require('../svg/settings'),
  SVGWarning = require('../svg/warning'),
  Collapsible = require('../collapsible'),
  CsrfTokenHiddenInput = require('../csrf_token_hidden_input'),
  BrowserUtils = require('../browser_utils'),
  Event = require('../event'),
  ImmutableObjectUtils = require('../immutable_object_utils'),
  debounce = require('javascript-debounce'),
  Sort = require('../sort'),
  Magic8Ball = require('../magic_8_ball'),
  oauth2ApplicationShape = require('./oauth2_application_shape');
  require('codemirror/mode/markdown/markdown');

var AWSEnvVariableStrings = {
  accessKeyName: "AWS Access Key",
  secretKeyName: "AWS Secret Key",
  regionName: "AWS Region"
};

var magic8BallResponse = Magic8Ball.response();

return React.createClass({
  displayName: 'BehaviorEditor',

  propTypes: {
    teamId: React.PropTypes.string.isRequired,
    groupName: React.PropTypes.string,
    groupDescription: React.PropTypes.string,
    behavior: React.PropTypes.instanceOf(BehaviorVersion).isRequired,
    csrfToken: React.PropTypes.string.isRequired,
    justSaved: React.PropTypes.bool,
    envVariables: React.PropTypes.arrayOf(React.PropTypes.object),
    otherBehaviorsInGroup: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
    paramTypes: React.PropTypes.arrayOf(
      React.PropTypes.shape({
        id: React.PropTypes.string.isRequired,
        name: React.PropTypes.string.isRequired,
        needsConfig: React.PropTypes.bool.isRequired
      })
    ).isRequired,
    oauth2Applications: React.PropTypes.arrayOf(oauth2ApplicationShape),
    oauth2Apis: React.PropTypes.arrayOf(React.PropTypes.shape({
      apiId: React.PropTypes.string.isRequired,
      name: React.PropTypes.string.isRequired
    })),
    simpleTokenApis: React.PropTypes.arrayOf(React.PropTypes.shape({
      apiId: React.PropTypes.string.isRequired,
      name: React.PropTypes.string.isRequired
    })),
    linkedOAuth2ApplicationIds: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
    notifications: React.PropTypes.arrayOf(React.PropTypes.object),
    shouldRevealCodeEditor: React.PropTypes.bool,
    onSave: React.PropTypes.func.isRequired,
    onLoad: React.PropTypes.func
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

  getOtherSavedParametersInGroup: function() {
    const currentInputIds = this.getBehaviorParams().map(ea => ea.inputId);
    const all = this.props.otherBehaviorsInGroup.reduce((arr, ea) => {
      return arr.concat(ea.params);
    }, [])
      .filter(ea => currentInputIds.indexOf(ea.inputId) === -1)
      .filter(ea => ea.isSaved())
      .map(ea => ea.clone({ groupId: this.props.behavior.groupId }));
    return UniqueBy.forArray(all, 'inputId');
  },

  getAllOAuth2Applications: function() {
    return this.props.oauth2Applications || [];
  },

  getRequiredOAuth2ApiConfigs: function() {
    if (this.state) {
      return this.getBehaviorConfig()['requiredOAuth2ApiConfigs'] || [];
    } else if (this.props.behavior.config) {
      return this.props.behavior.config.requiredOAuth2ApiConfigs || [];
    } else {
      return [];
    }
  },

  getAllSimpleTokenApis: function() {
    return this.props.simpleTokenApis || [];
  },

  getRequiredSimpleTokenApis: function() {
    if (this.state) {
      return this.getBehaviorConfig()['requiredSimpleTokenApis'] || [];
    } else if (this.props.behavior.config) {
      return this.props.behavior.config.requiredSimpleTokenApis || [];
    } else {
      return [];
    }
  },

  getApiApplications: function() {
    return this.getRequiredOAuth2ApiConfigs()
      .filter((config) => !!config.application)
      .map((config) => config.application);
  },

  getAWSConfig: function() {
    if (this.state) {
      return this.getBehaviorConfig()['aws'];
    } else if (this.props.behavior.config) {
      return this.props.behavior.config.aws;
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

  getBehaviorDescription: function() {
    return this.getBehaviorProp('description') || "";
  },

  getBehaviorFunctionBody: function() {
    return this.getBehaviorProp('functionBody') || "";
  },

  getBehaviorParams: function() {
    return this.getBehaviorProp('params') || [];
  },

  getBehaviorProp: function(key) {
    if (this.state) {
      return this.state.behavior[key];
    } else {
      return this.props.behavior[key];
    }
  },

  getBehaviorTemplate: function() {
    var template = this.getBehaviorProp('responseTemplate');
    if ((!template || !template.text) && !this.hasModifiedTemplate() && !this.isDataTypeBehavior()) {
      return this.getDefaultBehaviorTemplate();
    } else {
      return template;
    }
  },

  getBehaviorTriggers: function() {
    return this.getBehaviorProp('triggers') || this.getInitialTriggersFromBehavior(this.props.behavior);
  },

  getBehaviorConfig: function() {
    return this.getBehaviorProp('config');
  },

  getDataTypeName: function() {
    return this.getBehaviorConfig().dataTypeName;
  },

  shouldForcePrivateResponse: function() {
    return !!this.getBehaviorConfig().forcePrivateResponse;
  },

  getCodeAutocompletions: function() {
    var apiTokens = this.getApiApplications().map((application) => `ellipsis.accessTokens.${application.keyName}`);

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
    var isUsingCode = this.state ? this.state.revealCodeEditor : this.shouldRevealCodeEditor();
    return new ResponseTemplate({
      text: isUsingCode ? 'The answer is: {successResult}.' : magic8BallResponse
    });
  },

  getEnvVariables: function() {
    if (this.state) {
      return this.state.envVariables;
    } else {
      return this.getInitialEnvVariables();
    }
  },

  getEnvVariableNames: function() {
    return this.getEnvVariables().map(function(ea) {
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
        <span className="mobile-display-none">{this.isDataTypeBehavior() ? "Manage data type" : "Manage skill"}</span>
        <span className="mobile-display-only">Manage</span>
      </span>
    );
  },

  getRedirectValue: function() {
    return this.state.redirectValue;
  },

  getFormAction: function() {
    return jsRoutes.controllers.BehaviorEditorController.save().url;
  },

  buildEnvVarNotifications: function() {
    return this.getEnvVariables().
      filter((ea) => this.props.behavior.knownEnvVarsUsed.includes(ea.name)).
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

  getOAuth2ApplicationsRequiringAuth: function() {
    return this.getApiApplications().filter(ea => {
      return !this.props.linkedOAuth2ApplicationIds.includes(ea.applicationId);
    });
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

  getParamTypesNeedingConfiguration: function() {
    const paramTypes = Array.from(new Set(this.getBehaviorParams().map(ea => ea.paramType)));
    return paramTypes.filter(ea => ea.needsConfig);
  },

  buildDataTypeNotifications: function() {
    var notifications = [];
    this.getParamTypesNeedingConfiguration().forEach(ea => {
      notifications.push({
        kind: "data_type_needs_config",
        name: ea.name,
        link: jsRoutes.controllers.BehaviorEditorController.edit(ea.id).url
      });
    });
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
    return Object.keys(triggerParamObj).map((name) => {
      if (Param.isValidName(name)) {
        return {
          kind: "param_not_in_function",
          name: name,
          onClick: () => {
            this.addParams([name]);
          }
        };
      } else {
        return {
          kind: "invalid_param_in_trigger",
          name: name
        };
      }
    });
  },

  getValidParamNamesForTemplate: function() {
    return this.getBehaviorParams().map((param) => param.name)
      .concat(this.getSystemParams())
      .concat('successResult');
  },

  buildTemplateNotifications: function() {
    var template = this.getBehaviorTemplate();
    var validParams = this.getValidParamNamesForTemplate();
    var unknownTemplateParams = template.getUnknownParamsExcluding(validParams);
    return unknownTemplateParams.map((paramName) => ({
      kind: "unknown_param_in_template",
      name: paramName
    }));
  },

  buildNotifications: function() {
    var serverNotifications = this.props.notifications || [];
    var allNotifications = serverNotifications.concat(
      this.buildEnvVarNotifications(),
      this.buildOAuthApplicationNotifications(),
      this.buildDataTypeNotifications(),
      this.buildParamNotifications(),
      this.buildTemplateNotifications()
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

  getInitialTriggersFromBehavior: function(behavior) {
    if (behavior.triggers && behavior.triggers.length > 0) {
      return behavior.triggers;
    } else if (!this.isDataTypeBehavior()) {
      return [new Trigger()];
    } else {
      return [];
    }
  },

  getLastLineNumberForCode: function() {
    var numLines = this.getBehaviorFunctionBody().split('\n').length;
    return this.getFirstLineNumberForCode() + numLines;
  },

  getNotifications: function() {
    return this.state.notifications;
  },

  getTimestampedBehavior: function(behavior) {
    return behavior.clone({ createdAt: Date.now() });
  },

  getVersions: function() {
    return this.state.versions;
  },

  getResponseTemplateSectionNumber: function() {
    var hasParams = this.hasUserParameters();
    var hasCode = this.state.revealCodeEditor;
    if (hasParams && hasCode) {
      return "4";
    } else if (hasParams || hasCode) {
      return "3";
    } else {
      return "2";
    }
  },

  /* Setters/togglers */

  createNewParam: function(optionalValues) {
    return new Param(Object.assign({ paramType: this.props.paramTypes[0] }, optionalValues));
  },

  addParam: function(param) {
    var newParams = this.getBehaviorParams().concat([param]);
    this.setBehaviorProp('params', newParams, this.focusOnLastParam);
  },

  addNewParam: function() {
    var newParamIndex = this.getBehaviorParams().length + 1;
    while (this.getBehaviorParams().some(function(param) {
      return param.name === 'userInput' + newParamIndex;
    })) {
      newParamIndex++;
    }
    this.addParam(this.createNewParam({ name: 'userInput' + newParamIndex }));
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

  confirmDeleteBehaviorGroup: function() {
    this.toggleActivePanel('confirmDeleteBehaviorGroup', true);
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

  deleteBehaviorGroup: function() {
    this.refs.deleteBehaviorGroupForm.submit();
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

  fixLeftPanelPosition: function() {
    var form = this.refs.behaviorForm;
    var panel = this.refs.leftPanel;
    var scrim = this.refs.scrim.getElement();
    if (form && panel && scrim) {
      var topStyle = `${form.offsetTop}px`;
      panel.style.top = topStyle;
      scrim.style.top = topStyle;
    }
  },

  getHeaderHeight: function() {
    var mainHeader = document.getElementById('main-header');
    return mainHeader ? mainHeader.offsetHeight : 0;
  },

  fixHeaderHeight: debounce(function() {
    if (this.refs.pageTitle) {
      this.refs.pageTitle.style.top = `${this.getHeaderHeight()}px`;
    }
  }, 50),

  getFixedTitleHeight: function() {
    if (this.refs.pageTitle) {
      return this.refs.pageTitle.offsetHeight;
    } else {
      return 0;
    }
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
    var url = jsRoutes.controllers.BehaviorEditorController.versionInfoFor(this.props.behavior.behaviorId).url;
    this.setState({
      versionsLoadStatus: 'loading'
    });
    fetch(url, { credentials: 'same-origin' })
      .then((response) => response.json())
      .then((json) => {
        var behaviorVersions = json.map((version) => {
          return BehaviorVersion.fromJson(version);
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
    if (Event.keyPressWasEsc(event)) {
      this.handleEscKey(event);
    } else if (Event.keyPressWasSaveShortcut(event)) {
      event.preventDefault();
      if (this.isModified()) {
        this.refs.saveButton.focus();
        this.onSaveBehavior();
      }
    }
  },

  onSaveError: function() {
    this.setState({
      activePanel: null,
      error: "not_saved"
    });
  },

  backgroundSave: function(optionalCallback) {
    var form = new FormData(this.refs.behaviorForm);
    fetch(this.getFormAction(), {
      credentials: 'same-origin',
      method: 'POST',
      headers: {
        'Accept': 'application/json',
        'Csrf-Token': this.props.csrfToken,
        'x-requested-with': 'XMLHttpRequest'
      },
      body: form
    }).then((response) => response.json())
      .then((json) => {
        if (json.behaviorId) {
          let newProps = Object.assign({}, json, { onLoad: optionalCallback });
          this.props.onSave(newProps, true);
        } else {
          this.onSaveError();
        }
      })
      .catch((error) => {
        this.onSaveError(error);
      });
  },

  checkDataAndCallback: function(callback) {
    if (this.getBehaviorTemplate().toString() === this.getDefaultBehaviorTemplate().toString()) {
      this.setBehaviorProp('responseTemplate', this.getBehaviorTemplate(), callback);
    } else {
      callback();
    }
  },

  onSaveClick: function() {
    this.onSaveBehavior();
  },

  onSaveBehavior: function(optionalCallback) {
    this.setState({ error: null });
    this.toggleActivePanel('saving', true);
    this.checkDataAndCallback(() => { this.backgroundSave(optionalCallback); });
  },

  submitForm: function() {
    this.refs.behaviorForm.submit();
  },

  showVersionIndex: function(versionIndex, optionalCallback) {
    var version = this.getVersions()[versionIndex];
    var newBehavior = version.clone({
      groupId: this.props.behavior.groupId,
      teamId: this.props.behavior.teamId,
      behaviorId: this.props.behavior.behaviorId
    });
    this.setState({
      behavior: newBehavior,
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
    var newBehavior = this.state.behavior.clone(props);
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
    window.location = jsRoutes.controllers.BehaviorImportExportController.export(this.props.behavior.groupId).url;
  },

  toggleActiveDropdown: function(name) {
    var alreadyOpen = this.getActiveDropdown() === name;
    this.setState({
      activeDropdown: alreadyOpen ? null : { name: name }
    });
  },

  toggleActivePanel: function(name, beModal, optionalCallback) {
    var alreadyOpen = this.getActivePanel() === name;
    if (!alreadyOpen) {
      this.refs.scrim.getElement().style.top = '';
    }
    this.setState({
      activePanel: alreadyOpen ? null : { name: name, modal: !!beModal }
    }, optionalCallback || function() {
      var activeModal = this.getActiveModalElement();
      if (activeModal) {
        this.focusOnPrimaryOrFirstPossibleElement(activeModal);
      }
    });
  },

  clearActivePanel: function() {
    this.setState({
      activePanel: null
    });
  },

  toggleSharedAnswerInputSelector: function() {
    this.toggleActivePanel('sharedAnswerInputSelector', true);
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

  toggleBehaviorSwitcher: function() {
    this.toggleActivePanel('behaviorSwitcher', true, () => {
      if (this.getActivePanel() === 'behaviorSwitcher') {
        this.fixLeftPanelPosition();
        this.refs.behaviorSwitcher.focus();
      }
    });
  },

  checkIfModifiedAndTest: function() {
    const ref = this.isDataTypeBehavior() ? 'dataTypeTester' : 'behaviorTester';
    if (this.isModified()) {
      this.onSaveBehavior(() => {
        this.toggleTester(ref);
      });
    } else {
      this.toggleTester(ref);
    }
  },

  toggleTester: function(ref) {
    this.toggleActivePanel(ref, true, () => {
      if (this.getActivePanel() === ref) {
        this.refs[ref].focus();
      }
    });
  },

  toggleBehaviorTester: function() {
    this.toggleTester('behaviorTester');
  },

  toggleDataTypeTester: function() {
    this.toggleTester('dataTypeTester');
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

  updateDataTypeName: function(newName) {
    this.setBehaviorProp('config', Object.assign({}, this.getBehaviorConfig(), { dataTypeName: newName }));
  },

  updateDataTypeResultConfig: function(shouldUseSearch) {
    if (shouldUseSearch) {
      var searchQueryParam = this.createNewParam({ name: 'searchQuery' });
      this.setBehaviorProp('params', [searchQueryParam]);
    } else {
      this.setBehaviorProp('params', []);
    }
  },

  updateDescription: function(newDescription) {
    this.setBehaviorProp('description', newDescription);
  },

  updateEnvVariables: function(envVars, options) {
    var url = jsRoutes.controllers.EnvironmentVariablesController.submit().url;
    var data = {
      teamId: this.props.teamId,
      variables: envVars
    };
    fetch(url, this.jsonPostOptions({ teamId: this.props.teamId, dataJson: JSON.stringify(data) }))
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

  onBehaviorGroupNameChange: function(name) {
    this.setState({
      groupName: name
    });
  },

  onBehaviorGroupDescriptionChange: function(desc) {
    this.setState({
      groupDescription: desc
    });
  },

  jsonPostOptions: function(data) {
    return {
      credentials: 'same-origin',
      method: 'POST',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'Csrf-Token': this.props.csrfToken
      },
      body: JSON.stringify(data)
    };
  },

  saveBehaviorGroupName: function() {
    var url = jsRoutes.controllers.BehaviorEditorController.saveBehaviorGroupName().url;
    var data = {
      groupId: this.props.behavior.groupId,
      name: this.state.groupName
    };
    // TODO: error handling!
    fetch(url, this.jsonPostOptions(data)).then(() => {
      this.setState({ lastSavedGroupName: this.state.groupName });
    });
  },

  saveBehaviorGroupDescription: function() {
    var url = jsRoutes.controllers.BehaviorEditorController.saveBehaviorGroupDescription().url;
    var data = {
      groupId: this.props.behavior.groupId,
      description: this.state.groupDescription
    };
    // TODO: error handling!
    fetch(url, this.jsonPostOptions(data)).then(() => {
      this.setState({ lastSavedGroupDescription: this.state.groupDescription });
    });
  },

  saveBehaviorGroupDetailChanges: function() {
    this.saveBehaviorGroupName();
    this.saveBehaviorGroupDescription();
  },

  cancelBehaviorGroupDetailChanges: function() {
    this.setState({
      groupName: this.state.lastSavedGroupName,
      groupDescription: this.state.lastSavedGroupDescription
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

  updateForcePrivateResponse: function(newValue) {
    this.setConfigProperty('forcePrivateResponse', newValue);
  },

  updateTemplate: function(newTemplateString) {
    this.setBehaviorProp('responseTemplate', this.getBehaviorTemplate().clone({ text: newTemplateString }), () => {
      this.setState({ hasModifiedTemplate: true });
    });
  },

  syncParamNamesAndCount: function(oldName, newName) {
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
    var newTemplate = oldTemplate.replaceParamName(oldName, newName);
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
    var newBehavior = this.getInitialBehavior(this.props.behavior);
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
    return this.state && this.state.hasModifiedTemplate;
  },

  getAllBehaviors: function() {
    return this.props.otherBehaviorsInGroup.concat([this.getTimestampedBehavior(this.state.behavior)]);
  },

  getActionBehaviors: function() {
    return this.getAllBehaviors().filter(ea => !ea.isDataType());
  },

  getDataTypeBehaviors: function() {
    return this.getAllBehaviors().filter(ea => ea.isDataType());
  },

  countActionBehaviorsInGroup: function() {
    return this.getActionBehaviors().length;
  },

  hasUserParameters: function() {
    return this.getBehaviorParams() && this.getBehaviorParams().length > 0;
  },

  hasUserParameterNamed: function(paramName) {
    return this.getBehaviorParams().some((param) => param.name === paramName);
  },

  isDataTypeBehavior: function() {
    const name = this.getDataTypeName();
    return name !== null && name !== undefined;
  },

  isSearchDataTypeBehavior: function() {
    return this.isDataTypeBehavior() && this.hasUserParameterNamed('searchQuery');
  },

  isExistingBehavior: function() {
    return !!this.props.behavior.behaviorId;
  },

  isFinishedBehavior: function() {
    return this.isExistingBehavior() && !!(this.props.behavior.functionBody || this.props.behavior.responseTemplate.text);
  },

  isModified: function() {
    var currentMatchesInitial = this.state.behavior.isIdenticalToVersion(this.getInitialBehavior(this.props.behavior));
    var previewingVersions = this.getActivePanel() === 'versionHistory';
    return !currentMatchesInitial && !previewingVersions;
  },

  isSaving: function() {
    return this.getActivePanel() === 'saving';
  },

  shouldFilterCurrentVersion: function() {
    var firstTwoVersions = this.getVersions().slice(0, 2);
    return firstTwoVersions.length === 2 &&
      firstTwoVersions[0].isIdenticalToVersion(firstTwoVersions[1]);
  },

  shouldRevealCodeEditor: function() {
    return !!(this.props.shouldRevealCodeEditor || this.props.behavior.functionBody);
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
    BrowserUtils.ensureYPosInView(cursorBottom, this.refs.footer.getHeight());
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

  onAddSimpleTokenApi: function(toAdd) {
    this.setConfigProperty('requiredSimpleTokenApis', this.getRequiredSimpleTokenApis().concat([toAdd]));
  },

  onRemoveSimpleTokenApi: function(toRemove) {
    var existing = this.getRequiredSimpleTokenApis();
    this.setConfigProperty('requiredSimpleTokenApis', existing.filter(function(ea) {
      return ea.apiId !== toRemove.apiId;
    }));
  },

  onNewOAuth2Application: function(requiredOAuth2ApiConfigId) {
    this.setState({
      redirectValue: "newOAuth2Application",
      requiredOAuth2ApiConfigId: requiredOAuth2ApiConfigId || ""
    }, () => { this.checkDataAndCallback(this.submitForm); });
  },

  onParamEnterKey: function(index) {
    if (index + 1 < this.getBehaviorParams().length) {
      this.focusOnParamIndex(index + 1);
    } else if (this.getBehaviorParams()[index].question) {
      this.addNewParam();
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
    window.addEventListener('resize', this.fixHeaderHeight, false);
    this.refs.pageTitleLayoutReplacer.style.height = `${this.getFixedTitleHeight()}px`;
  },

  getInitialBehavior: function(behavior) {
    return behavior.clone({
      triggers: this.getInitialTriggersFromBehavior(behavior)
    });
  },

  getInitialEnvVariables: function() {
    return Sort.arrayAlphabeticalBy(this.props.envVariables || [], (variable) => variable.name);
  },

  getInitialState: function() {
    var initialBehavior = this.getInitialBehavior(this.props.behavior);
    return {
      behavior: initialBehavior,
      groupName: this.props.groupName || "",
      groupDescription: this.props.groupDescription || "",
      lastSavedGroupName: this.props.groupName || "",
      lastSavedGroupDescription: this.props.groupDescription || "",
      activeDropdown: null,
      activePanel: null,
      codeEditorUseLineWrapping: false,
      justSaved: this.props.justSaved,
      envVariables: this.getInitialEnvVariables(),
      revealCodeEditor: this.shouldRevealCodeEditor(),
      hasModifiedTemplate: !!(this.props.behavior.responseTemplate && this.props.behavior.responseTemplate.text),
      notifications: this.buildNotifications(),
      versions: [this.getTimestampedBehavior(initialBehavior)],
      versionsLoadStatus: null,
      onNextNewEnvVar: null,
      envVariableAdderPrompt: null,
      redirectValue: "",
      requiredOAuth2ApiConfigId: "",
      paramNameToSync: null,
      error: null
    };
  },

  componentWillReceiveProps: function(nextProps) {
    var newBehaviorVersion = this.getInitialBehavior(nextProps.behavior);
    this.setState({
      activePanel: null,
      justSaved: true,
      behavior: newBehaviorVersion,
      versions: [this.getTimestampedBehavior(newBehaviorVersion)],
      versionsLoadStatus: null,
      error: null
    });
    if (typeof(nextProps.onLoad) === 'function') {
      nextProps.onLoad();
    }
    if (newBehaviorVersion.behaviorId) {
      BrowserUtils.replaceURL(jsRoutes.controllers.BehaviorEditorController.edit(newBehaviorVersion.behaviorId).url);
    }
  },

  renderHiddenFormValues: function() {
    return (
      <div>
        <CsrfTokenHiddenInput value={this.props.csrfToken} />
        <HiddenJsonInput value={JSON.stringify(this.state.behavior)} />
        <input type="hidden" name="redirect" value={this.getRedirectValue()} />
        <input type="hidden" name="requiredOAuth2ApiConfigId" value={this.state.requiredOAuth2ApiConfigId} />
      </div>
    );
  },

  renderCodeEditor: function() {
    return (
      <div>
        <div className="border-top border-left border-right border-light mtxxl mobile-mtn ptm">
          <div className="type-s">
            <div className="plxxxl prs mbm">
              <APISelectorMenu
                openWhen={this.getActiveDropdown() === 'apiSelectorDropdown'}
                onAWSClick={this.toggleAWSConfig}
                awsCheckedWhen={!!this.getAWSConfig()}
                toggle={this.toggleAPISelectorMenu}
                allOAuth2Applications={this.getAllOAuth2Applications()}
                requiredOAuth2ApiConfigs={this.getRequiredOAuth2ApiConfigs()}
                allSimpleTokenApis={this.getAllSimpleTokenApis()}
                requiredSimpleTokenApis={this.getRequiredSimpleTokenApis()}
                onAddOAuth2Application={this.onAddOAuth2Application}
                onRemoveOAuth2Application={this.onRemoveOAuth2Application}
                onAddSimpleTokenApi={this.onAddSimpleTokenApi}
                onRemoveSimpleTokenApi={this.onRemoveSimpleTokenApi}
                onNewOAuth2Application={this.onNewOAuth2Application}
                getOAuth2ApiWithId={this.getOAuth2ApiWithId}
              />
            </div>

            <Collapsible revealWhen={!!this.getAWSConfig()}>
              <div className="plxxxl prs pbs mbs border-bottom border-light">
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
          onCodeDelete={this.isDataTypeBehavior() ? null : this.confirmDeleteCode}
        />
      </div>
    );
  },

  renderFooter: function() {
    return (
      <div>
        <ModalScrim ref="scrim" isActive={this.hasModalPanel()} onClick={this.clearActivePanel} />
        <FixedFooter ref="footer" className={(this.isModified() ? "bg-white" : "bg-light-translucent")}>
          <Collapsible ref="confirmUndo" revealWhen={this.getActivePanel() === 'confirmUndo'}>
            <ConfirmActionPanel confirmText="Undo changes" onConfirmClick={this.undoChanges} onCancelClick={this.hideActivePanel}>
              <p>This will undo any changes you’ve made since last saving. Are you sure you want to do this?</p>
            </ConfirmActionPanel>
          </Collapsible>

          <Collapsible ref="confirmDeleteBehavior" revealWhen={this.getActivePanel() === 'confirmDeleteBehavior'}>
            <ConfirmActionPanel confirmText="Delete" onConfirmClick={this.deleteBehavior} onCancelClick={this.hideActivePanel}>
              <p>Are you sure you want to delete this action?</p>
            </ConfirmActionPanel>
          </Collapsible>

          <Collapsible ref="confirmDeleteBehaviorGroup" revealWhen={this.getActivePanel() === 'confirmDeleteBehaviorGroup'}>
            <ConfirmActionPanel confirmText="Delete" onConfirmClick={this.deleteBehaviorGroup} onCancelClick={this.hideActivePanel}>
              <p>Are you sure you want to delete this skill and all of its actions and data types?</p>
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
              apiAccessTokens={this.getApiApplications()}
              onAddNewEnvVariable={this.onAddNewEnvVariable}
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
            <div className="box-action phn">
              <div className="container">
                <div className="columns">
                  <div className="column column-page-sidebar"></div>
                  <div className="column column-page-main">
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
            <div className="box-action phn">
              <div className="container">
                <div className="columns">
                  <div className="column column-page-sidebar"></div>
                  <div className="column column-page-main">
                    <EnvVariableAdder
                      ref="envVariableAdderPanel"
                      onCancelClick={this.hideActivePanel}
                      onSave={this.addEnvVar}
                      prompt={this.state.envVariableAdderPrompt}
                      existingNames={this.getEnvVariableNames()}
                    />
                  </div>
                </div>
              </div>
            </div>
          </Collapsible>

          <Collapsible revealWhen={this.getActivePanel() === 'behaviorTester'}>
            <BehaviorTester
              ref="behaviorTester"
              triggers={this.getBehaviorTriggers()}
              params={this.getBehaviorParams()}
              behaviorId={this.props.behavior.behaviorId}
              csrfToken={this.props.csrfToken}
              onDone={this.toggleBehaviorTester}
              appsRequiringAuth={this.getOAuth2ApplicationsRequiringAuth()}
            />
          </Collapsible>

          <Collapsible revealWhen={this.getActivePanel() === 'dataTypeTester'}>
            <DataTypeTester
              ref="dataTypeTester"
              behaviorId={this.props.behavior.behaviorId}
              isSearch={this.isSearchDataTypeBehavior()}
              csrfToken={this.props.csrfToken}
              onDone={this.toggleDataTypeTester}
              appsRequiringAuth={this.getOAuth2ApplicationsRequiringAuth()}
            />
          </Collapsible>

          {this.getOtherSavedParametersInGroup().length > 0 ? (
            <Collapsible revealWhen={this.getActivePanel() === 'sharedAnswerInputSelector'}>
              <SharedAnswerInputSelector
                ref="sharedAnswerInputSelector"
                onToggle={this.toggleSharedAnswerInputSelector}
                onSelect={this.addParam}
                params={this.getOtherSavedParametersInGroup()}
              />
            </Collapsible>
          ) : null}

          <Collapsible ref="saving" revealWhen={this.isSaving()}>
            <div className="box-action">
              <div className="container phn">
                <p className="align-c">
                  <b className="pulse">Saving changes…</b>
                </p>
              </div>
            </div>
          </Collapsible>

          <Collapsible revealWhen={!this.hasModalPanel()}>
            {this.getNotifications().map((notification, index) => (
              <Notification key={"notification" + index} notification={notification} />
            ))}
            <div className="container container-wide ptm">
              <div className="columns columns-elastic mobile-columns-float">
                <div className="column column-expand mobile-column-auto">
                  <DynamicLabelButton
                    ref="saveButton"
                    onClick={this.onSaveClick}
                    labels={[{
                      text: 'Save changes',
                      mobileText: 'Save',
                      displayWhen: !this.state.justSaved
                    }, {
                      text: 'Saved',
                      displayWhen: this.state.justSaved
                    }]}
                    className="button-primary mrs mbm"
                    disabledWhen={!this.isModified() || this.isSaving()}
                  />
                  <button className="mrs mbm" type="button" disabled={!this.isModified() || this.isSaving()} onClick={this.confirmUndo}>
                    <span className="mobile-display-none">Undo changes</span>
                    <span className="mobile-display-only">Undo</span>
                  </button>
                  <DynamicLabelButton
                    labels={[{
                      text: 'Test…',
                      displayWhen: !this.isModified()
                    }, {
                      text: 'Save and test…',
                      displayWhen: this.isModified()
                    }]}
                    disabledWhen={!this.isExistingBehavior() && !this.isModified()}
                    className="mrl mbm" onClick={this.checkIfModifiedAndTest}
                  />
                  <div className="display-inline-block align-button mbm type-bold type-italic">
                    {this.renderFooterStatus()}
                  </div>
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
                      <DropdownMenu.Item onClick={this.exportVersion} label="Export this skill" />
                      <DropdownMenu.Item onClick={this.cloneBehavior} label="Clone this action" />
                      <DropdownMenu.Item onClick={this.confirmDeleteBehavior} label="Delete this action" />
                      <DropdownMenu.Item onClick={this.confirmDeleteBehaviorGroup} label="Delete the whole skill" />
                    </DropdownMenu>
                  ) : null}
                </div>
              </div>
            </div>
          </Collapsible>

        </FixedFooter>
      </div>
    );
  },

  renderFooterStatus: function() {
    if (this.state.justSaved && !this.isSaving()) {
      return (
        <span className="fade-in type-green">All changes saved</span>
      );
    } else if (this.state.error === 'not_saved') {
      return (
        <span className="fade-in type-pink">
          <span style={{ height: 24 }} className="display-inline-block mrs align-b"><SVGWarning /></span>
          <span>Error saving changes — please try again</span>
        </span>
      );
    } else if (this.isModified()) {
      return (
        <span className="fade-in type-pink">Unsaved changes</span>
      );
    } else {
      return "";
    }
  },

  renderHiddenForms: function() {
    return (
      <div>
        <form className="pbxxxl" ref="deleteBehaviorForm" action={jsRoutes.controllers.BehaviorEditorController.delete().url} method="POST">
          <CsrfTokenHiddenInput value={this.props.csrfToken} />
          <input type="hidden" name="behaviorId" value={this.props.behavior.behaviorId} />
        </form>

        <form className="pbxxxl" ref="deleteBehaviorGroupForm" action={jsRoutes.controllers.ApplicationController.deleteBehaviorGroups().url} method="POST">
          <CsrfTokenHiddenInput value={this.props.csrfToken} />
          <input type="hidden" name="behaviorGroupIds[0]" value={this.props.behavior.groupId} />
        </form>

        <form className="pbxxxl" ref="cloneBehaviorForm" action={jsRoutes.controllers.BehaviorEditorController.duplicate().url} method="POST">
          <CsrfTokenHiddenInput value={this.props.csrfToken} />
          <input type="hidden" name="behaviorId" value={this.props.behavior.behaviorId} />
        </form>
      </div>
    );
  },

  getPageName: function(optionalName, actionCount) {
    if (optionalName) {
      return (
        <span className="type-black">{optionalName}</span>
      );
    } else if (actionCount > 1) {
      return 'Untitled skill';
    } else if (actionCount === 1 && this.isExistingBehavior()) {
      return 'Skill';
    } else if (actionCount === 1 && !this.isExistingBehavior()) {
      return 'New skill';
    } else {
      return 'Edit data type';
    }
  },

  getPageSummary: function(actionCount, dataTypeCount) {
    if (actionCount === 1 && dataTypeCount === 1) {
      return `1 action, 1 data type`;
    } else if (actionCount === 1 && dataTypeCount > 1) {
      return `1 action, ${dataTypeCount} data types`;
    } else if (actionCount > 1 && dataTypeCount === 0) {
      return `${actionCount} actions`;
    } else if (actionCount > 1 && dataTypeCount === 1) {
      return `${actionCount} actions, 1 data type`;
    } else if (actionCount > 1 && dataTypeCount > 1) {
      return `${actionCount} actions, ${dataTypeCount} data types`;
    } else if (actionCount === 0 && dataTypeCount > 1) {
      return `${dataTypeCount} data types`;
    } else if (actionCount === 1 && dataTypeCount === 0 && this.isExistingBehavior()) {
      return `1 action`;
    } else if (actionCount === 0 && dataTypeCount === 1 && this.isExistingBehavior()) {
      return `1 data type`;
    } else {
      return null;
    }
  },

  getPageDescription: function(optionalDescription, actionCount, dataTypeCount) {
    var summary = this.getPageSummary(actionCount, dataTypeCount);
    if (optionalDescription && summary) {
      return (
        <span>
          <span className="mhs mobile-display-none">·</span>
          <span className="type-black mobile-display-none">{optionalDescription}</span>
          <span className="mhs">·</span>
          <i>{summary}</i>
        </span>
      );
    } else if (optionalDescription && !summary) {
      return (
        <span className="mobile-display-none">
          <span className="mhs">·</span>
          <span className="type-black">{optionalDescription}</span>
        </span>
      );
    } else if (summary) {
      return (
        <span>
          <span className="mhs">·</span>
          <i>{summary}</i>
        </span>
      );
    } else {
      return null;
    }
  },

  getPageHeading: function() {
    var actionCount = this.getActionBehaviors().length;
    var dataTypeCount = this.getDataTypeBehaviors().length;

    return (
      <span>
        <span className="align-m">
          {this.getPageName(this.state.lastSavedGroupName, actionCount)}
        </span>
        <span className="type-m type-regular align-m">
          {this.getPageDescription(this.state.lastSavedGroupDescription, actionCount, dataTypeCount)}
        </span>
      </span>
    );
  },

  getBehaviorHeading: function() {
    if (this.getAllBehaviors().length > 1) {
      return (
        <h3 className="type-blue-faded mtl mbn">{this.isDataTypeBehavior() ? "Edit data type" : "Edit action"}</h3>
      );
    } else {
      return null;
    }
  },

  shouldShowBehaviorSwitcher: function() {
    return !!this.props.behavior.groupId;
  },

  renderPageHeadingContent: function() {
    if (this.shouldShowBehaviorSwitcher()) {
      return (
        <button type="button" className="button-tab button-tab-subtle" onClick={this.toggleBehaviorSwitcher}>
          <span className="display-inline-block align-t mrm" style={{ height: "24px" }}>
            <SVGHamburger />
          </span>
          <h4 className="display-inline-block align-m man">{this.getPageHeading()}</h4>
        </button>
      );
    } else {
      return (
        <h4 className="man">{this.getPageHeading()}</h4>
      );
    }
  },

  renderPageHeading: function() {
    return (
      <div>
        <div ref="pageTitle"
          className="bg-white-translucent border-bottom position-fixed-top position-z-almost-front display-ellipsis display-limit-width"
          style={{ top: `${this.getHeaderHeight()}px` }}
        >
          <div className="container container-wide pts type-weak">
            {this.renderPageHeadingContent()}
          </div>
        </div>
        <div ref="pageTitleLayoutReplacer" style={{ height: `${this.getFixedTitleHeight()}px` }}></div>
      </div>
    );
  },

  renderBehaviorSwitcher: function() {
    if (this.shouldShowBehaviorSwitcher()) {
      return (
        <div ref="leftPanel" className={"position-fixed-left position-z-front bg-white-translucent " +
        (this.getActivePanel() === 'behaviorSwitcher' ? "border-right" : "")}>
          <Collapsible revealWhen={this.getActivePanel() === 'behaviorSwitcher'} isHorizontal={true}>
            <BehaviorSwitcher
              ref="behaviorSwitcher"
              onToggle={this.toggleBehaviorSwitcher}
              actionBehaviors={this.getActionBehaviors()}
              dataTypeBehaviors={this.getDataTypeBehaviors()}
              currentBehavior={this.getTimestampedBehavior(this.state.behavior)}
              groupId={this.props.behavior.groupId}
              groupName={this.state.groupName}
              lastSavedGroupName={this.state.lastSavedGroupName}
              groupDescription={this.state.groupDescription}
              lastSavedGroupDescription={this.state.lastSavedGroupDescription}
              teamId={this.props.teamId}
              onBehaviorGroupNameChange={this.onBehaviorGroupNameChange}
              onBehaviorGroupDescriptionChange={this.onBehaviorGroupDescriptionChange}
              onSaveBehaviorGroupDetails={this.saveBehaviorGroupDetailChanges}
              onCancelBehaviorGroupDetails={this.cancelBehaviorGroupDetailChanges}
            />
          </Collapsible>
        </div>
      );
    } else {
      return null;
    }
  },

  renderNormalBehavior: function() {
    return (

      <div>
        {this.renderPageHeading()}

        {this.renderBehaviorSwitcher()}

        <form action={this.getFormAction()} method="POST" ref="behaviorForm">

        {this.renderHiddenFormValues()}

        {/* Start of container */}
        <div className="pbxxxl">

          <div className="container pts">
            {this.getBehaviorHeading()}

            <Input
              className="form-input-borderless form-input-m type-bold mbn"
              placeholder="Add a description (optional)"
              onChange={this.updateDescription}
              value={this.getBehaviorDescription()}
            />
          </div>

          <hr className="mtneg1 mbn thin bg-gray-light" />

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
            onParamAdd={this.addNewParam}
            onParamNameFocus={this.onParamNameFocus}
            onParamNameBlur={this.onParamNameBlur}
            onEnterKey={this.onParamEnterKey}
            userParams={this.getBehaviorParams()}
            paramTypes={this.props.paramTypes}
            triggers={this.getBehaviorTriggers()}
            isFinishedBehavior={this.isFinishedBehavior()}
            behaviorHasCode={this.state.revealCodeEditor}
            hasSharedAnswers={this.getOtherSavedParametersInGroup().length > 0}
            onToggleSharedAnswer={this.toggleSharedAnswerInputSelector}
          />

          <Collapsible revealWhen={this.state.revealCodeEditor} animationDuration={0}>
            <hr className="man thin bg-gray-light" />
          </Collapsible>

          <Collapsible revealWhen={!this.state.revealCodeEditor}>
            <div className="bg-blue-lighter border-top border-bottom border-blue pvl">
              <div className="container container-wide">
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
            </div>
          </Collapsible>

          <Collapsible revealWhen={this.state.revealCodeEditor} animationDuration={0.5}>

            <div className="columns container container-wide flex-columns mobile-flex-no-columns">
              <div className="flex-column flex-column-left column column-page-sidebar mbxxl mobile-mbs ptxxl">
                <CodeEditorHelp
                  sectionNumber={this.hasUserParameters() ? "3" : "2"}
                  isFinishedBehavior={this.isFinishedBehavior()}
                  functionBody={this.getBehaviorFunctionBody()}
                  onToggleHelp={this.toggleBoilerplateHelp}
                  helpIsActive={this.getActivePanel() === 'helpForBoilerplateParameters'}
                  hasUserParameters={this.hasUserParameters()}
                />
              </div>

              <div className="flex-column flex-column-left column column-page-main column-page-main-wide">
                {this.renderCodeEditor()}
              </div>
            </div>

            <hr className="man full-bleed thin bg-gray-light" />
          </Collapsible>

          <ResponseTemplateConfiguration
            template={this.getBehaviorTemplate()}
            onChangeTemplate={this.updateTemplate}
            isFinishedBehavior={this.isFinishedBehavior()}
            behaviorUsesCode={!!this.state.revealCodeEditor}
            shouldForcePrivateResponse={this.shouldForcePrivateResponse()}
            onChangeForcePrivateResponse={this.updateForcePrivateResponse}
            onCursorChange={this.ensureCursorVisible}
            userParams={this.getBehaviorParams()}
            sectionNumber={this.getResponseTemplateSectionNumber()}
          />

        </div> {/* End of container */}

        {this.renderFooter()}

      </form>

        {this.renderHiddenForms()}

      </div>
    );
  },

  renderDataTypeBehavior: function() {
    return (
      <div>
        {this.renderPageHeading()}

        {this.renderBehaviorSwitcher()}

        <form action={this.getFormAction()} method="POST" ref="behaviorForm">
          {this.renderHiddenFormValues()}

          <div className="container pts">
            {this.getBehaviorHeading()}
          </div>

          <DataTypeNameInput
            name={this.getDataTypeName()}
            onChange={this.updateDataTypeName}
          />

          <hr className="man thin bg-gray-light" />

          <DataTypeResultConfig
            usesSearch={this.hasUserParameterNamed('searchQuery')}
            onChange={this.updateDataTypeResultConfig}
          />

          <hr className="man thin bg-gray-light" />

          <div className="container container-wide pbxxxl">
            <div className="columns flex-columns mobile-flex-no-columns">
              <div className="flex-column flex-column-left column column-page-sidebar ptxl mbxxl mobile-mbs">

                <SectionHeading number="3">Run code to generate a list</SectionHeading>

                <DataTypeCodeEditorHelp
                  functionBody={this.getBehaviorFunctionBody()}
                  usesSearch={this.hasUserParameterNamed('searchQuery')}
                />

              </div>

              <div className="flex-column flex-column-left column column-page-main column-page-main-wide mbxxl">
                {this.renderCodeEditor()}
              </div>
            </div>
          </div>

          {this.renderFooter()}
        </form>

        {this.renderHiddenForms()}

      </div>
    );
  },

  render: function() {
    if (this.isDataTypeBehavior()) {
      return this.renderDataTypeBehavior();
    } else {
      return this.renderNormalBehavior();
    }
  }
});

});
