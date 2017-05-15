define((require) => {
var React = require('react'),
  APISelectorMenu = require('./api_selector_menu'),
  AWSHelp = require('./aws_help'),
  BehaviorGroup = require('../models/behavior_group'),
  BehaviorGroupEditor = require('./behavior_group_editor'),
  BehaviorVersion = require('../models/behavior_version'),
  BehaviorSwitcher = require('./behavior_switcher'),
  BehaviorTester = require('./behavior_tester'),
  DataTypeTester = require('./data_type_tester'),
  BoilerplateParameterHelp = require('./boilerplate_parameter_help'),
  ChangeSummary = require('./change_summary'),
  CodeConfiguration = require('./code_configuration'),
  CodeEditorHelp = require('./code_editor_help'),
  ConfirmActionPanel = require('../panels/confirm_action'),
  CollapseButton = require('../shared_ui/collapse_button'),
  DataTypeCodeEditorHelp = require('./data_type_code_editor_help'),
  DataTypeResultConfig = require('./data_type_result_config'),
  DynamicLabelButton = require('../form/dynamic_label_button'),
  EnvVariableAdder = require('../environment_variables/adder'),
  EnvVariableSetter = require('../environment_variables/setter'),
  FixedFooter = require('../shared_ui/fixed_footer'),
  HiddenJsonInput = require('./hidden_json_input'),
  Input = require('../models/input'),
  NotificationData = require('../models/notification_data'),
  FormInput = require('../form/input'),
  LibraryCodeEditorHelp = require('./library_code_editor_help'),
  LibraryVersion = require('../models/library_version'),
  ModalScrim = require('../shared_ui/modal_scrim'),
  Notifications = require('../notifications/notifications'),
  PageWithPanels = require('../shared_ui/page_with_panels'),
  ResponseTemplate = require('../models/response_template'),
  ResponseTemplateConfiguration = require('./response_template_configuration'),
  ResponseTemplateHelp = require('./response_template_help'),
  SavedAnswerEditor = require('./saved_answer_editor'),
  SharedAnswerInputSelector = require('./shared_answer_input_selector'),
  Sticky = require('../shared_ui/sticky'),
  SVGHamburger = require('../svg/hamburger'),
  Trigger = require('../models/trigger'),
  TriggerConfiguration = require('./trigger_configuration'),
  TriggerHelp = require('./trigger_help'),
  UniqueBy = require('../lib/unique_by'),
  UserInputConfiguration = require('./user_input_configuration'),
  VersionsPanel = require('./versions_panel'),
  SVGWarning = require('../svg/warning'),
  Collapsible = require('../shared_ui/collapsible'),
  CsrfTokenHiddenInput = require('../shared_ui/csrf_token_hidden_input'),
  BrowserUtils = require('../lib/browser_utils'),
  Event = require('../lib/event'),
  ImmutableObjectUtils = require('../lib/immutable_object_utils'),
  debounce = require('javascript-debounce'),
  Sort = require('../lib/sort'),
  Magic8Ball = require('../lib/magic_8_ball'),
  oauth2ApplicationShape = require('./oauth2_application_shape');
  require('codemirror/mode/markdown/markdown');

var AWSEnvVariableStrings = {
  accessKeyName: "AWS Access Key",
  secretKeyName: "AWS Secret Key",
  regionName: "AWS Region"
};

var magic8BallResponse = Magic8Ball.response();

var MOBILE_MAX_WIDTH = 768;

const BehaviorEditor = React.createClass({
  displayName: 'BehaviorEditor',

  propTypes: Object.assign(PageWithPanels.requiredPropTypes(), {
    group: React.PropTypes.instanceOf(BehaviorGroup).isRequired,
    selectedId: React.PropTypes.string,
    csrfToken: React.PropTypes.string.isRequired,
    envVariables: React.PropTypes.arrayOf(React.PropTypes.object),
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
    savedAnswers: React.PropTypes.arrayOf(
      React.PropTypes.shape({
        inputId: React.PropTypes.string.isRequired,
        userAnswerCount: React.PropTypes.number.isRequired,
        myValueString: React.PropTypes.string
      })
    ).isRequired,
    onSave: React.PropTypes.func.isRequired,
    onForgetSavedAnswerForInput: React.PropTypes.func.isRequired,
    onLoad: React.PropTypes.func
  }),


  /* Getters */

  getActiveDropdown: function() {
    return this.state.activeDropdown && this.state.activeDropdown.name ? this.state.activeDropdown.name : "";
  },

  otherBehaviorsInGroup: function() {
    return this.getBehaviorGroup().behaviorVersions.filter(ea => ea.behaviorId !== this.getSelectedId());
  },

  getOtherSavedInputsInGroup: function() {
    const currentInputIds = this.getInputs().map(ea => ea.inputId);
    const allInputIds = this.otherBehaviorsInGroup().reduce((arr, ea) => {
      return arr.concat(ea.inputIds);
    }, []);
    const otherInputIds = allInputIds.filter(ea => currentInputIds.indexOf(ea.inputId) === -1);
    const otherInputs = otherInputIds.map(eaId => this.getBehaviorGroup().getInputs().find(ea => ea.inputId === eaId));
    const otherSavedInputs = otherInputs.filter(ea => ea.isSaved());
    return UniqueBy.forArray(otherSavedInputs, 'inputId');
  },

  getAllOAuth2Applications: function() {
    return this.props.oauth2Applications || [];
  },

  getRequiredOAuth2ApiConfigs: function() {
    return this.getBehaviorGroup().getRequiredOAuth2ApiConfigs();
  },

  getAllSimpleTokenApis: function() {
    return this.props.simpleTokenApis || [];
  },

  getRequiredSimpleTokenApis: function() {
    return this.getBehaviorGroup().getRequiredSimpleTokenApis();
  },

  getApiApplications: function() {
    return this.getRequiredOAuth2ApiConfigs()
      .filter((config) => !!config.application)
      .map((config) => config.application);
  },

  getAWSConfig: function() {
    var selectedBehavior = this.getSelectedBehavior();
    if (this.state) {
      var config = this.getBehaviorConfig();
      return config && config['aws'];
    } else if (selectedBehavior && selectedBehavior.config) {
      return selectedBehavior.config.aws;
    } else {
      return undefined;
    }
  },

  getBehaviorName: function() {
    return this.getEditableProp('name') || "";
  },

  getEditableDescription: function() {
    return this.getEditableProp('description') || "";
  },

  getFunctionBody: function() {
    return this.getEditableProp('functionBody') || "";
  },

  getInputIds: function() {
    return this.getEditableProp('inputIds') || [];
  },

  getInputs: function() {
    const allInputs = this.getBehaviorGroup().getInputs();
    return this.getInputIds().
      map(eaId => allInputs.find(ea => ea.inputId === eaId)).
      filter(ea => !!ea);
  },

  getFirstBehaviorInputName: function() {
    var inputs = this.getInputs();
    if (inputs[0] && inputs[0].name) {
      return inputs[0].name;
    } else {
      return "";
    }
  },

  getSelectedId: function() {
    if (this.state) {
      return this.state.selectedId;
    } else {
      return this.props.selectedId;
    }
  },

  getBehaviorGroup: function() {
    if (this.state) {
      return this.state.group;
    } else {
      return this.props.group;
    }
  },

  getOriginalSelected: function() {
    return this.getSelectedFor(this.props.group, this.getSelectedId());
  },

  getSelectedBehavior: function() {
    const selected = this.getSelected();
    return (selected && selected.isBehaviorVersion()) ? selected : null;
  },

  getSelectedFor: function(group, selectedId) {
    return group.getEditables().find(ea => {
      return ea.getPersistentId() === selectedId;
    });
  },

  getSelectedLibrary: function() {
    const selected = this.getSelected();
    return (selected && selected.isLibraryVersion()) ? selected : null;
  },

  getSelected: function() {
    return this.getSelectedFor(this.getBehaviorGroup(), this.getSelectedId());
  },

  getEditableProp: function(key) {
    var selected = this.getSelected();
    return selected ? selected[key] : null;
  },

  getBehaviorTemplate: function() {
    var selectedBehavior = this.getSelectedBehavior();
    if (!selectedBehavior) {
      return null;
    }
    var template = this.getEditableProp('responseTemplate');
    if ((!template || !template.text) && !this.hasModifiedTemplate() && !this.isDataTypeBehavior()) {
      return this.getDefaultBehaviorTemplate();
    } else {
      return template;
    }
  },

  getBehaviorTriggers: function() {
    return this.getEditableProp('triggers') || [];
  },

  getBehaviorConfig: function() {
    return this.getEditableProp('config');
  },

  shouldForcePrivateResponse: function() {
    return !!this.getBehaviorConfig().forcePrivateResponse;
  },

  getSystemParams: function() {
    return ["ellipsis"];
  },

  getDefaultBehaviorTemplate: function() {
    return new ResponseTemplate({
      text: this.getSelectedBehavior().shouldRevealCodeEditor ? 'The answer is: {successResult}.' : magic8BallResponse
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

  getInputWithSavedAnswers: function() {
    if (this.state.selectedSavedAnswerInputId) {
      return this.getInputs().find(ea => ea.inputId === this.state.selectedSavedAnswerInputId);
    } else {
      return null;
    }
  },

  getFormAction: function() {
    return jsRoutes.controllers.BehaviorEditorController.save().url;
  },

  buildEnvVarNotifications: function() {
    var selectedBehavior = this.getSelectedBehavior();
    if (selectedBehavior) {
      return this.getEnvVariables()
        .filter((ea) => selectedBehavior.knownEnvVarsUsed.includes(ea.name))
        .filter((ea) => !ea.isAlreadySavedWithValue)
        .map((ea) => new NotificationData({
          kind: "env_var_not_defined",
          environmentVariableName: ea.name,
          onClick: () => {
            this.showEnvVariableSetter(ea.name);
          }
        }));
    } else {
      return [];
    }
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
    const behavior = this.getSelectedBehavior();
    if (!behavior) {
      return [];
    }
    return this.getRequiredOAuth2ApiConfigsWithNoApplication().map(ea => new NotificationData({
      kind: "oauth2_config_without_application",
      name: this.getOAuth2ApiWithId(ea.apiId).name,
      requiredApiConfig: ea,
      existingOAuth2Applications: this.getAllOAuth2Applications(),
      onAddOAuth2Application: this.onAddOAuth2Application,
      onNewOAuth2Application: this.onNewOAuth2Application
    }));
  },

  getParamTypesNeedingConfiguration: function() {
    const paramTypes = Array.from(new Set(this.getInputs().map(ea => ea.paramType)));
    return paramTypes.filter(ea => ea.needsConfig);
  },

  buildDataTypeNotifications: function() {
    return this.getParamTypesNeedingConfiguration().map(ea => {
      const behaviorVersion = this.getBehaviorGroup().behaviorVersions.find(bv => bv.id === ea.id);
      const behaviorId = behaviorVersion ? behaviorVersion.behaviorId : null;
      return new NotificationData({
        kind: "data_type_needs_config",
        name: ea.name,
        onClick: () => this.onSelect(this.getBehaviorGroup().id, behaviorId)
      });
    });
  },

  getValidParamNamesForTemplate: function() {
    return this.getInputs().map((param) => param.name)
      .concat(this.getSystemParams())
      .concat('successResult');
  },

  buildTemplateNotifications: function() {
    var selectedBehavior = this.getSelectedBehavior();
    if (selectedBehavior) {
      var template = this.getBehaviorTemplate();
      var validParams = this.getValidParamNamesForTemplate();
      var unknownTemplateParams = template.getUnknownParamsExcluding(validParams);
      return unknownTemplateParams.map((paramName) => new NotificationData({
        kind: "unknown_param_in_template",
        name: paramName
      }));
    } else {
      return [];
    }
  },

  buildNotifications: function() {
    return [].concat(
      this.buildEnvVarNotifications(),
      this.buildOAuthApplicationNotifications(),
      this.buildDataTypeNotifications(),
      this.buildTemplateNotifications()
    );
  },

  getNotifications: function() {
    return this.state.notifications;
  },

  getVersions: function() {
    return this.state.versions;
  },

  getResponseTemplateSectionNumber: function() {
    var hasInputs = this.hasInputs();
    var hasCode = this.getSelectedBehavior().shouldRevealCodeEditor;
    if (hasInputs && hasCode) {
      return "4";
    } else if (hasInputs || hasCode) {
      return "3";
    } else {
      return "2";
    }
  },

  getParamTypes: function() {
    var customTypes = Sort.arrayAlphabeticalBy(this.getBehaviorGroup().getCustomParamTypes(), (ea) => ea.name);
    return this.props.builtinParamTypes.concat(customTypes);
  },

  /* Setters/togglers */

  setBehaviorInputs: function(newBehaviorInputs, callback) {
    const newGroup = this.getBehaviorGroup().copyWithInputsForBehaviorVersion(newBehaviorInputs, this.getSelectedBehavior());
    this.updateGroupStateWith(newGroup, callback);
  },

  addInput: function(input) {
    const newInputs = this.getInputs().concat([input]);
    this.setBehaviorInputs(newInputs, this.focusOnLastInput);
  },

  getNewGenericInputName: function() {
    let newIndex = this.getInputs().length + 1;
    while (this.getInputs().some(ea => {
      return ea.name === 'userInput' + newIndex;
    })) {
      newIndex++;
    }
    return `userInput${newIndex}`;
  },

  addNewInput: function(optionalNewName) {
    const newName = optionalNewName || this.getNewGenericInputName();
    const url = jsRoutes.controllers.BehaviorEditorController.newUnsavedInput(newName).url;
    fetch(url, { credentials: 'same-origin' })
      .then(response => response.json())
      .then(json => {
        this.addInput(new Input(json));
      });
  },

  addTrigger: function(callback) {
    this.setEditableProp('triggers', this.getBehaviorTriggers().concat(new Trigger()), callback);
  },

  cancelVersionPanel: function() {
    this.props.onClearActivePanel();
    this.showVersionIndex(0);
  },

  cloneBehavior: function() {
    const behaviorVersion = this.getSelectedBehavior();
    if (behaviorVersion) {
      this.addNewBehavior(behaviorVersion.isDataType(), behaviorVersion.behaviorId);
    }
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
    this.props.onClearActivePanel();
    const group = this.getBehaviorGroup();
    const updatedGroup = group.clone({
      behaviorVersions: group.behaviorVersions.filter(ea => ea.behaviorId !== this.getSelectedId())
    });
    this.updateGroupStateWith(updatedGroup);
  },

  deleteBehaviorGroup: function() {
    this.refs.deleteBehaviorGroupForm.submit();
  },

  deleteCode: function() {
    this.setEditableProp('inputIds', []);
    this.setEditableProp('functionBody', '');
    this.toggleCodeEditor();
    this.props.onClearActivePanel();
  },

  deleteInputAtIndex: function(index) {
    this.setEditableProp('inputIds', ImmutableObjectUtils.arrayRemoveElementAtIndex(this.getInputIds(), index));
  },

  deleteTriggerAtIndex: function(index) {
    var triggers = ImmutableObjectUtils.arrayRemoveElementAtIndex(this.getBehaviorTriggers(), index);
    this.setEditableProp('triggers', triggers);
  },

  getLeftPanelCoordinates: function() {
    var headerHeight = this.getHeaderHeight();
    var footerHeight = this.getFooterHeight();
    var windowHeight = window.innerHeight;

    var availableHeight = windowHeight - headerHeight - footerHeight;
    var newHeight = availableHeight > 0 ? availableHeight : window.innerHeight;
    return {
      top: headerHeight,
      left: window.scrollX > 0 ? -window.scrollX : 0,
      bottom: newHeight
    };
  },

  hasMobileLayout: function() {
    return this.state.hasMobileLayout;
  },

  windowIsMobile: function() {
    return window.innerWidth <= MOBILE_MAX_WIDTH;
  },

  checkMobileLayout: function() {
    if (this.hasMobileLayout() !== this.windowIsMobile()) {
      this.setState({
        behaviorSwitcherVisible: this.isExistingGroup() && !this.windowIsMobile(),
        hasMobileLayout: this.windowIsMobile()
      });
    }
  },

  layoutDidUpdate: function() {
    var panel = this.refs.leftPanel;
    if (panel) {
      panel.resetCoordinates();
    }
  },

  getHeaderHeight: function() {
    var mainHeader = document.getElementById('main-header');
    return mainHeader ? mainHeader.offsetHeight : 0;
  },

  getFooterHeight: function() {
    var mainFooter = this.refs.footer;
    return mainFooter ? mainFooter.getHeight() : 0;
  },

  updateBehaviorScrollPosition: function() {
    if (this.getSelected()) {
      this.setEditableProp('editorScrollPosition', window.scrollY);
    }
  },

  refreshCodeEditor: function() {
    this.refs.codeEditor.refresh();
  },

  loadVersions: function() {
    var url = jsRoutes.controllers.BehaviorEditorController.versionInfoFor(this.getBehaviorGroup().id).url;
    this.setState({
      versionsLoadStatus: 'loading'
    });
    fetch(url, { credentials: 'same-origin' })
      .then((response) => response.json())
      .then((json) => {
        var versions = json.map((version) => {
          return BehaviorGroup.fromJson(version);
        });
        this.setState({
          versions: this.state.versions.concat(versions),
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
    }
  },

  hideActiveDropdown: function() {
    this.setState({
      activeDropdown: null
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
        this.onSaveBehaviorGroup();
      }
    }
  },

  onSaveError: function(error) { // eslint-disable-line no-unused-vars
    this.props.onClearActivePanel();
    this.setState({
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
        if (json.id) {
          if (this.state.shouldRedirectToAddNewOAuth2App) {
            const config = this.state.requiredOAuth2ApiConfig;
            const apiId = config && config.apiId;
            const recommendedScope = config && config.recommendedScope;
            window.location.href = jsRoutes.controllers.OAuth2ApplicationController.newApp(apiId, recommendedScope, null, this.getSelectedId()).url;
          } else {
            const newProps = {
              group: BehaviorGroup.fromJson(json),
              onLoad: optionalCallback
            };
            this.props.onSave(newProps, this.state);
          }
        } else {
          this.onSaveError();
        }
      })
      .catch((error) => {
        this.onSaveError(error);
      });
  },

  isJustSaved: function() {
    return this.getBehaviorGroup().isRecentlySaved() && !this.isModified();
  },

  checkDataAndCallback: function(callback) {
    var template = this.getBehaviorTemplate();
    if (template && template.toString() === this.getDefaultBehaviorTemplate().toString()) {
      this.setEditableProp('responseTemplate', this.getBehaviorTemplate(), callback);
    } else {
      callback();
    }
  },

  onSaveClick: function() {
    this.onSaveBehaviorGroup();
  },

  onSaveBehaviorGroup: function(optionalCallback) {
    this.setState({ error: null });
    this.toggleActivePanel('saving', true);
    this.checkDataAndCallback(() => { this.backgroundSave(optionalCallback); });
  },

  showVersionIndex: function(versionIndex, optionalCallback) {
    const version = this.getVersions()[versionIndex];
    const stateUpdates = {
      group: version
    };
    if (!version.hasBehaviorVersionWithId(this.getSelectedId())) {
      stateUpdates.selectedId = null;
    }
    this.setState(stateUpdates, optionalCallback);
  },

  restoreVersionIndex: function(versionIndex) {
    this.showVersionIndex(versionIndex, function() {
      this.onSaveBehaviorGroup();
    });
  },

  setAWSEnvVar: function(property, envVarName) {
    var awsConfig = Object.assign({}, this.getAWSConfig());
    awsConfig[property] = envVarName;
    this.setConfigProperty('aws', awsConfig);
  },

  setEditableProp: function(key, value, callback) {
    var newProps = {};
    newProps[key] = value;
    this.setEditableProps(newProps, callback);
  },

  buildNewVersionsWithBehaviorProps(group, behavior, props) {
    const timestampedBehavior = behavior.clone(props).copyWithNewTimestamp();
    return group.behaviorVersions.
      filter(ea => ea.behaviorId !== timestampedBehavior.behaviorId ).
      concat([timestampedBehavior]);
  },

  setEditableProps: function(props, callback) {
    const existingGroup = this.getBehaviorGroup();
    const existingSelected = this.getSelectedFor(existingGroup, this.getSelectedId());
    if (existingSelected) {
      const updatedGroup = existingSelected.buildUpdatedGroupFor(existingGroup, props);
      this.updateGroupStateWith(updatedGroup, callback);
    }
  },

  getNextBehaviorIdFor: function(group) {
    if (group.behaviorVersions.length) {
      return group.behaviorVersions[0].behaviorId;
    } else {
      return null;
    }
  },

  updateGroupStateWith: function(updatedGroup, callback) {

    const timestampedGroup = updatedGroup.copyWithNewTimestamp();
    const selectedIdBefore = this.getSelectedId();
    const selectedIdNowInvalid = !timestampedGroup.behaviorVersions.find(ea => ea.behaviorId === selectedIdBefore) && !timestampedGroup.libraryVersions.find(ea => ea.libraryId === selectedIdBefore);
    let selectedIdAfter;
    if (selectedIdBefore && selectedIdNowInvalid) {
      selectedIdAfter = this.getNextBehaviorIdFor(timestampedGroup);
    } else {
      selectedIdAfter = selectedIdBefore;
    }

    const updatedVersions = ImmutableObjectUtils.arrayWithNewElementAtIndex(this.state.versions, timestampedGroup, 0);

    this.setState({
      group: timestampedGroup,
      versions: updatedVersions,
      selectedId: selectedIdAfter
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
    this.setEditableProp('config', config, callback);
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

  toggleActiveDropdown: function(name) {
    var alreadyOpen = this.getActiveDropdown() === name;
    this.setState({
      activeDropdown: alreadyOpen ? null : { name: name }
    });
  },

  toggleActivePanel: function(name, beModal, optionalCallback) {
    var alreadyOpen = this.props.activePanelName === name;
    if (!alreadyOpen) {
      this.refs.scrim.getElement().style.top = '';
    }
    this.props.onToggleActivePanel(name, beModal, optionalCallback);
  },

  toggleSharedAnswerInputSelector: function() {
    this.toggleActivePanel('sharedAnswerInputSelector', true);
  },

  toggleAWSConfig: function() {
    this.setConfigProperty('aws', this.getAWSConfig() ? undefined : {});
  },

  toggleBehaviorSwitcher: function() {
    this.setState({
      behaviorSwitcherVisible: !this.state.behaviorSwitcherVisible
    });
  },

  checkIfModifiedAndTest: function() {
    const ref = this.isDataTypeBehavior() ? 'dataTypeTester' : 'behaviorTester';
    if (this.isModified()) {
      this.onSaveBehaviorGroup(() => {
        this.props.onClearActivePanel(() => {
          this.toggleTester(ref);
        });
      });
    } else {
      this.props.onClearActivePanel(() => {
        this.toggleTester(ref);
      });
    }
  },

  toggleTester: function(ref) {
    this.toggleActivePanel(ref, true, () => {
      if (this.props.activePanelName === ref) {
        this.refs[ref].focus();
      }
    });
  },

  toggleBoilerplateHelp: function() {
    this.toggleActivePanel('helpForBoilerplateParameters');
  },

  toggleCodeEditor: function() {
    const updatedBehaviorVersions = this.getBehaviorGroup().behaviorVersions.map(ea => {
      if (ea.behaviorId === this.getSelectedId()) {
        return ea.clone({ shouldRevealCodeEditor: !ea.shouldRevealCodeEditor });
      } else {
        return ea;
      }
    });
    const updatedGroup = this.getBehaviorGroup().clone({ behaviorVersions: updatedBehaviorVersions });
    this.updateGroupStateWith(updatedGroup, () => {
      this.resetNotifications();
      this.refreshCodeEditor();
    });
  },

  toggleCodeEditorLineWrapping: function() {
    this.setState({
      codeEditorUseLineWrapping: !this.state.codeEditorUseLineWrapping
    });
  },

  toggleResponseTemplateHelp: function() {
    this.toggleActivePanel('helpForResponseTemplate');
  },

  toggleSavedAnswerEditor: function(savedAnswerId) {
    if (this.props.activePanelName === 'savedAnswerEditor') {
      this.toggleActivePanel('savedAnswerEditor', true, () => {
        this.setState({ selectedSavedAnswerInputId: null });
      });
    } else {
      this.setState({ selectedSavedAnswerInputId: savedAnswerId }, () => {
        this.toggleActivePanel('savedAnswerEditor', true, () => null);
      });
    }
  },

  toggleTriggerHelp: function() {
    this.toggleActivePanel('helpForTriggerParameters');
  },

  toggleVersionListMenu: function() {
    this.toggleActiveDropdown('versionList');
  },

  updateCode: function(newCode) {
    this.setEditableProp('functionBody', newCode);
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

  updateDataTypeResultConfig: function(shouldUseSearch) {
    if (shouldUseSearch) {
      this.addNewInput('searchQuery');
    } else {
      this.setEditableProp('inputIds', []);
    }
  },

  updateDescription: function(newDescription) {
    this.setEditableProp('description', newDescription);
  },

  updateName: function(newName) {
    this.setEditableProp('name', newName);
  },

  updateEnvVariables: function(envVars, options) {
    var url = jsRoutes.controllers.EnvironmentVariablesController.submit().url;
    var data = {
      teamId: this.getBehaviorGroup().teamId,
      variables: envVars
    };
    fetch(url, this.jsonPostOptions({ teamId: this.getBehaviorGroup().teamId, dataJson: JSON.stringify(data) }))
      .then((response) => response.json())
      .then((json) => {
        this.props.onClearActivePanel();
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

  forgetSavedAnswerRequest: function(url, inputId) {
    var data = {
      inputId: inputId
    };
    fetch(url, this.jsonPostOptions(data))
      .then((response) => response.json())
      .then((json) => {
        if (json.numDeleted > 0) {
          this.props.onForgetSavedAnswerForInput(inputId, json.numDeleted);
        }
      });
  },

  forgetSavedAnswerForUser: function(inputId) {
    var url = jsRoutes.controllers.SavedAnswerController.resetForUser().url;
    this.forgetSavedAnswerRequest(url, inputId);
  },

  forgetSavedAnswersForTeam: function(inputId) {
    var url = jsRoutes.controllers.SavedAnswerController.resetForTeam().url;
    this.forgetSavedAnswerRequest(url, inputId);
  },

  onBehaviorGroupNameChange: function(name) {
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ name: name }));
  },

  onBehaviorGroupDescriptionChange: function(desc) {
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ description: desc  }));
  },

  onBehaviorGroupIconChange: function(icon) {
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ icon: icon  }));
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

  updateBehaviorInputAtIndexWith: function(index, newInput) {
    var oldInputs = this.getInputs();
    var oldInputName = oldInputs[index].name;
    var newInputName = newInput.name;
    var newInputs = ImmutableObjectUtils.arrayWithNewElementAtIndex(oldInputs, newInput, index);

    this.setBehaviorInputs(newInputs, () => {
      var numTriggersReplaced = 0;
      if (oldInputName === this.state.paramNameToSync) {
        numTriggersReplaced = this.syncParamNamesAndCount(oldInputName, newInputName);
        if (numTriggersReplaced > 0) {
          this.setState({ paramNameToSync: newInputName });
        }
      }
    });
  },

  updateForcePrivateResponse: function(newValue) {
    this.setConfigProperty('forcePrivateResponse', newValue);
  },

  updateTemplate: function(newTemplateString) {
    this.setEditableProp('responseTemplate', this.getBehaviorTemplate().clone({ text: newTemplateString }), () => {
      this.setState({ hasModifiedTemplate: true });
    });
  },

  syncParamNamesAndCount: function(oldName, newName) {
    var numTriggersModified = 0;

    var newTriggers = this.getBehaviorTriggers().map((oldTrigger) => {
      if (oldTrigger.usesInputName(oldName)) {
        numTriggersModified++;
        return oldTrigger.clone({ text: oldTrigger.getTextWithNewInputName(oldName, newName) });
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
      this.setEditableProps(newProps);
    }

    return numTriggersModified + (templateModified ? 1 : 0);
  },

  updateTriggerAtIndexWithTrigger: function(index, newTrigger) {
    this.setEditableProp('triggers', ImmutableObjectUtils.arrayWithNewElementAtIndex(this.getBehaviorTriggers(), newTrigger, index));
  },

  undoChanges: function() {
    this.updateGroupStateWith(this.props.group, () => {
      this.props.onClearActivePanel();
      this.resetNotifications();
    });
  },

  /* Booleans */

  hasModifiedTemplate: function() {
    return this.state && this.state.hasModifiedTemplate;
  },

  getActionBehaviors: function() {
    return this.getBehaviorGroup().getActions();
  },

  getDataTypeBehaviors: function() {
    return this.getBehaviorGroup().getDataTypes();
  },

  getLibraries: function() {
    return this.getBehaviorGroup().libraryVersions;
  },

  hasInputs: function() {
    return this.getInputs() && this.getInputs().length > 0;
  },

  hasInputNamed: function(name) {
    return this.getInputs().some(ea => ea.name === name);
  },

  isDataTypeBehavior: function() {
    return this.getSelectedBehavior() && this.getSelectedBehavior().isDataType();
  },

  isSearchDataTypeBehavior: function() {
    return this.isDataTypeBehavior() && this.hasInputNamed('searchQuery');
  },

  isExistingBehavior: function() {
    return !!this.getSelectedBehavior() && !this.getSelectedBehavior().isNewBehavior;
  },

  isExistingGroup: function() {
    return !!this.getBehaviorGroup().id;
  },

  isFinishedBehavior: function() {
    var originalSelectedBehavior = this.getOriginalSelected();
    return !!(originalSelectedBehavior && !originalSelectedBehavior.isNewBehavior &&
      (originalSelectedBehavior.functionBody || originalSelectedBehavior.responseTemplate.text));
  },

  isFinishedLibraryVersion: function() {
    var originalSelected = this.getOriginalSelected();
    return !!(originalSelected && !originalSelected.isNewBehavior && originalSelected.functionBody);
  },

  isModified: function() {
    var currentMatchesInitial = this.props.group.isIdenticalTo(this.getBehaviorGroup());
    var previewingVersions = this.props.activePanelName === 'versionHistory';
    return !currentMatchesInitial && !previewingVersions;
  },

  editableIsModified: function(current) {
    var original = this.props.group.getEditables().find((ea) => ea.getPersistentId() === current.getPersistentId());
    var previewingVersions = this.props.activePanelName === 'versionHistory';
    return !previewingVersions && !(original && current.isIdenticalToVersion(original));
  },

  isSaving: function() {
    return this.props.activePanelName === 'saving';
  },

  shouldFilterCurrentVersion: function() {
    var firstTwoVersions = this.getVersions().slice(0, 2);
    return firstTwoVersions.length === 2 &&
      firstTwoVersions[0].isIdenticalTo(firstTwoVersions[1]);
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

  focusOnInputIndex: function(index) {
    if (this.refs.userInputConfiguration) {
      this.refs.userInputConfiguration.focusIndex(index);
    }
  },

  focusOnLastInput: function() {
    this.focusOnInputIndex(this.getInputs().length - 1);
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

  onAddOAuth2Application: function(appToAdd) {
    const existing = this.getRequiredOAuth2ApiConfigs();
    const indexToReplace = existing.findIndex(ea => ea.apiId === appToAdd.apiId && !ea.application);
    const toReplace = existing[indexToReplace];
    const configs = existing.slice();
    if (indexToReplace >= 0) {
      configs.splice(indexToReplace, 1);
    }
    const toAdd = Object.assign({}, toReplace, {
      apiId: appToAdd.apiId,
      application: appToAdd
    });
    const newConfigs =  configs.concat([toAdd]);
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ requiredOAuth2ApiConfigs: newConfigs }));
  },

  onRemoveOAuth2Application: function(appToRemove) {
    const existing = this.getRequiredOAuth2ApiConfigs();
    const newConfigs = existing.filter(function(config) {
      return config.application && config.application.applicationId !== appToRemove.applicationId;
    });
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ requiredOAuth2ApiConfigs: newConfigs }));
  },

  onAddSimpleTokenApi: function(toAdd) {
    const newConfigs = this.getRequiredSimpleTokenApis().concat([toAdd]);
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ requiredSimpleTokenApis: newConfigs }));
  },

  onRemoveSimpleTokenApi: function(toRemove) {
    const newConfigs = this.getRequiredSimpleTokenApis().filter(function(ea) {
      return ea.apiId !== toRemove.apiId;
    });
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ requiredSimpleTokenApis: newConfigs }));
  },

  onNewOAuth2Application: function(requiredOAuth2ApiConfig) {
    this.setState({
      shouldRedirectToAddNewOAuth2App: true,
      requiredOAuth2ApiConfig: requiredOAuth2ApiConfig
    }, () => { this.checkDataAndCallback(this.onSaveBehaviorGroup); });
  },

  onInputEnterKey: function(index) {
    if (index + 1 < this.getInputs().length) {
      this.focusOnInputIndex(index + 1);
    } else if (this.getInputs()[index].question) {
      this.addNewInput();
    }
  },

  onConfigureType: function(paramTypeId) {
    const typeBehaviorVersion = this.getBehaviorGroup().behaviorVersions.find(ea => ea.id === paramTypeId);
    if (typeBehaviorVersion) {
      this.onSelect(this.getBehaviorGroup().id, typeBehaviorVersion.behaviorId);
    }
  },

  onInputNameFocus: function(index) {
    this.setState({
      paramNameToSync: this.getInputs()[index].name
    });
  },

  onInputNameBlur: function() {
    this.setState({
      paramNameToSync: null
    });
  },

  resetNotificationsImmediately: function() {
    this.setState({
      notifications: this.buildNotifications()
    });
  },

  resetNotifications: debounce(function() {
    this.resetNotificationsImmediately();
  }, 250),

    /* Component API methods */
  componentDidMount: function() {
    window.document.addEventListener('click', this.onDocumentClick, false);
    window.document.addEventListener('keydown', this.onDocumentKeyDown, false);
    window.addEventListener('resize', this.checkMobileLayout, false);
    window.addEventListener('scroll', debounce(this.updateBehaviorScrollPosition, 500), false);
  },

  // componentDidUpdate: function() {
  // },

  getInitialEnvVariables: function() {
    return Sort.arrayAlphabeticalBy(this.props.envVariables || [], (variable) => variable.name);
  },

  getInitialState: function() {
    const selectedBehavior = this.getSelectedBehavior();
    const hasModifiedTemplate = !!(selectedBehavior && selectedBehavior.responseTemplate && selectedBehavior.responseTemplate.text);
    return {
      group: this.props.group,
      selectedId: this.props.selectedId,
      activeDropdown: null,
      codeEditorUseLineWrapping: false,
      envVariables: this.getInitialEnvVariables(),
      hasModifiedTemplate: hasModifiedTemplate,
      notifications: this.buildNotifications(),
      versions: [this.props.group.copyWithNewTimestamp()],
      versionsLoadStatus: null,
      onNextNewEnvVar: null,
      envVariableAdderPrompt: null,
      redirectValue: "",
      requiredOAuth2ApiConfig: null,
      shouldRedirectToAddNewOAuth2App: false,
      paramNameToSync: null,
      error: null,
      selectedSavedAnswerInputId: null,
      behaviorSwitcherVisible: this.isExistingGroup() && !this.windowIsMobile(),
      hasMobileLayout: this.windowIsMobile(),
      animationDisabled: false
    };
  },

  componentWillReceiveProps: function(nextProps) {
    if (nextProps.group !== this.props.group) {
      var newGroup = nextProps.group;
      var newState = {
        group: newGroup,
        versions: [newGroup.copyWithNewTimestamp()],
        versionsLoadStatus: null,
        error: null
      };
      if (!this.props.group.id && nextProps.group.id  && !this.windowIsMobile()) {
        newState.behaviorSwitcherVisible = true;
      }
      this.props.onClearActivePanel();
      this.setState(newState);
      if (typeof(nextProps.onLoad) === 'function') {
        nextProps.onLoad();
      }
      BrowserUtils.replaceURL(jsRoutes.controllers.BehaviorEditorController.edit(newGroup.id, this.getSelectedId()).url);
    }
  },

  renderHiddenFormValues: function() {
    return (
      <div>
        <CsrfTokenHiddenInput value={this.props.csrfToken} />
        <HiddenJsonInput value={JSON.stringify(this.getBehaviorGroup())} />
      </div>
    );
  },

  toggleAPISelectorMenu: function() {
    this.toggleActiveDropdown('apiSelectorDropdown');
  },

  renderAPISelector: function() {
    return (
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
    );
  },

  renderCodeEditor: function(props) {
    return (
      <CodeConfiguration
        ref="codeEditor"

        sectionNumber={props.sectionNumber}
        sectionHeading={props.sectionHeading}
        codeEditorHelp={props.codeEditorHelp}

        activePanelName={this.props.activePanelName}
        activeDropdownName={this.getActiveDropdown()}
        onToggleActiveDropdown={this.toggleActiveDropdown}
        onToggleActivePanel={this.toggleActivePanel}
        animationIsDisabled={this.animationIsDisabled()}

        onToggleAWSConfig={this.toggleAWSConfig}
        awsConfig={this.getAWSConfig()}
        onAWSAddNewEnvVariable={this.onAWSAddNewEnvVariable}
        onAWSConfigChange={this.setAWSEnvVar}

        apiSelector={this.renderAPISelector()}

        inputs={this.getInputs()}
        systemParams={props.systemParams || this.getSystemParams()}
        apiApplications={this.getApiApplications()}

        functionBody={this.getFunctionBody()}
        onChangeFunctionBody={this.updateCode}
        onCursorChange={this.ensureCursorVisible}
        useLineWrapping={this.state.codeEditorUseLineWrapping}
        onToggleCodeEditorLineWrapping={this.toggleCodeEditorLineWrapping}
        canDeleteFunctionBody={!this.isDataTypeBehavior() && !this.getSelectedLibrary()}
        onDeleteFunctionBody={this.confirmDeleteCode}

        envVariableNames={this.getEnvVariableNames()}

      />
    );
  },

  renderFooter: function() {
    return (
      <div>
        <ModalScrim ref="scrim"
          isActive={this.props.activePanelIsModal || this.mobileBehaviorSwitcherIsVisible()}
          onClick={this.props.onClearActivePanel}
        />
        <FixedFooter ref="footer" className={
          (this.mobileBehaviorSwitcherIsVisible() ? " mobile-position-behind-scrim " : "") +
          (this.isModified() ? " bg-white " : " bg-light-translucent ")
        }>
          <Collapsible ref="confirmUndo" revealWhen={this.props.activePanelName === 'confirmUndo'} onChange={this.layoutDidUpdate}>
            <ConfirmActionPanel confirmText="Undo changes" onConfirmClick={this.undoChanges} onCancelClick={this.props.onClearActivePanel}>
              <p>This will undo any changes you’ve made since last saving. Are you sure you want to do this?</p>
            </ConfirmActionPanel>
          </Collapsible>

          <Collapsible ref="confirmDeleteBehavior" revealWhen={this.props.activePanelName === 'confirmDeleteBehavior'} onChange={this.layoutDidUpdate}>
            <ConfirmActionPanel confirmText="Delete" onConfirmClick={this.deleteBehavior} onCancelClick={this.props.onClearActivePanel}>
              <p>Are you sure you want to delete this action?</p>
            </ConfirmActionPanel>
          </Collapsible>

          <Collapsible ref="confirmDeleteBehaviorGroup" revealWhen={this.props.activePanelName === 'confirmDeleteBehaviorGroup'} onChange={this.layoutDidUpdate}>
            <ConfirmActionPanel confirmText="Delete" onConfirmClick={this.deleteBehaviorGroup} onCancelClick={this.props.onClearActivePanel}>
              <p>Are you sure you want to delete this skill and all of its actions and data types?</p>
            </ConfirmActionPanel>
          </Collapsible>

          <Collapsible ref="confirmDeleteCode" revealWhen={this.props.activePanelName === 'confirmDeleteCode'} onChange={this.layoutDidUpdate}>
            <ConfirmActionPanel confirmText="Remove" onConfirmClick={this.deleteCode} onCancelClick={this.props.onClearActivePanel}>
              <p>Are you sure you want to remove all of the code?</p>
            </ConfirmActionPanel>
          </Collapsible>

          <Collapsible revealWhen={this.props.activePanelName === 'helpForTriggerParameters'} onChange={this.layoutDidUpdate}>
            <TriggerHelp onCollapseClick={this.props.onClearActivePanel} />
          </Collapsible>

          <Collapsible revealWhen={this.props.activePanelName === 'helpForBoilerplateParameters'} onChange={this.layoutDidUpdate}>
            <BoilerplateParameterHelp
              envVariableNames={this.getEnvVariableNames()}
              apiAccessTokens={this.getApiApplications()}
              onAddNewEnvVariable={this.onAddNewEnvVariable}
              onCollapseClick={this.props.onClearActivePanel}
            />
          </Collapsible>

          <Collapsible revealWhen={this.props.activePanelName === 'helpForResponseTemplate'} onChange={this.layoutDidUpdate}>
            <ResponseTemplateHelp
              firstParamName={this.getFirstBehaviorInputName()}
              template={this.getBehaviorTemplate()}
              onCollapseClick={this.props.onClearActivePanel}
            />
          </Collapsible>

          <Collapsible revealWhen={this.props.activePanelName === 'helpForAWS'} onChange={this.layoutDidUpdate}>
            <AWSHelp onCollapseClick={this.props.onClearActivePanel} />
          </Collapsible>

          <Collapsible ref="versionHistory" revealWhen={this.props.activePanelName === 'versionHistory'} onChange={this.layoutDidUpdate}>
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

          <Collapsible ref="envVariableSetter" revealWhen={this.props.activePanelName === 'envVariableSetter'} onChange={this.layoutDidUpdate}>
            <div className="box-action phn">
              <div className="container">
                <div className="columns">
                  <div className="column column-page-sidebar" />
                  <div className="column column-page-main">
                    <EnvVariableSetter
                      ref="envVariableSetterPanel"
                      vars={this.getEnvVariables()}
                      onCancelClick={this.props.onClearActivePanel}
                      onSave={this.updateEnvVariables}
                    />
                  </div>
                </div>
              </div>
            </div>
          </Collapsible>

          <Collapsible ref="envVariableAdder" revealWhen={this.props.activePanelName === 'envVariableAdder'} onChange={this.layoutDidUpdate}>
            <div className="box-action phn">
              <div className="container">
                <div className="columns">
                  <div className="column column-page-sidebar" />
                  <div className="column column-page-main">
                    <EnvVariableAdder
                      ref="envVariableAdderPanel"
                      onCancelClick={this.props.onClearActivePanel}
                      onSave={this.addEnvVar}
                      prompt={this.state.envVariableAdderPrompt}
                      existingNames={this.getEnvVariableNames()}
                    />
                  </div>
                </div>
              </div>
            </div>
          </Collapsible>

          <Collapsible revealWhen={this.props.activePanelName === 'behaviorTester'} onChange={this.layoutDidUpdate}>
            <BehaviorTester
              ref="behaviorTester"
              triggers={this.getBehaviorTriggers()}
              inputs={this.getInputs()}
              behaviorId={this.getSelectedId()}
              csrfToken={this.props.csrfToken}
              onDone={this.props.onClearActivePanel}
              appsRequiringAuth={this.getOAuth2ApplicationsRequiringAuth()}
            />
          </Collapsible>

          <Collapsible revealWhen={this.props.activePanelName === 'dataTypeTester'} onChange={this.layoutDidUpdate}>
            <DataTypeTester
              ref="dataTypeTester"
              behaviorId={this.getSelectedId()}
              isSearch={this.isSearchDataTypeBehavior()}
              csrfToken={this.props.csrfToken}
              onDone={this.props.onClearActivePanel}
              appsRequiringAuth={this.getOAuth2ApplicationsRequiringAuth()}
            />
          </Collapsible>

          {this.getOtherSavedInputsInGroup().length > 0 ? (
            <Collapsible revealWhen={this.props.activePanelName === 'sharedAnswerInputSelector'} onChange={this.layoutDidUpdate}>
              <SharedAnswerInputSelector
                ref="sharedAnswerInputSelector"
                onToggle={this.toggleSharedAnswerInputSelector}
                onSelect={this.addInput}
                inputs={this.getOtherSavedInputsInGroup()}
              />
            </Collapsible>
          ) : null}

          <Collapsible revealWhen={this.props.activePanelName === 'savedAnswerEditor'} onChange={this.layoutDidUpdate}>
            <SavedAnswerEditor
              ref="savedAnswerEditor"
              onToggle={this.toggleSavedAnswerEditor}
              savedAnswers={this.props.savedAnswers}
              selectedInput={this.getInputWithSavedAnswers()}
              onForgetSavedAnswerForUser={this.forgetSavedAnswerForUser}
              onForgetSavedAnswersForTeam={this.forgetSavedAnswersForTeam}
            />
          </Collapsible>

          <Collapsible ref="saving" revealWhen={this.isSaving()} onChange={this.layoutDidUpdate}>
            <div className="box-action">
              <div className="container phn">
                <p className="align-c">
                  <b className="pulse">Saving changes…</b>
                </p>
              </div>
            </div>
          </Collapsible>

          <Collapsible revealWhen={!this.props.activePanelIsModal} onChange={this.layoutDidUpdate} animationDisabled={this.animationIsDisabled()}>
            <Notifications notifications={this.getNotifications()} />
            <div className="container container-wide ptm border-top">
              <div>
                <div>
                  <DynamicLabelButton
                    ref="saveButton"
                    onClick={this.onSaveClick}
                    labels={[{
                      text: 'Save changes',
                      mobileText: 'Save',
                      displayWhen: !this.isJustSaved()
                    }, {
                      text: 'Saved',
                      displayWhen: this.isJustSaved()
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
                    className={`mbm ${this.isExistingGroup() ? "mrs" : "mrl"}`} onClick={this.checkIfModifiedAndTest}
                  />
                  {this.isExistingGroup() ? (
                    <button type="button"
                      className="mrl mbm"
                      onClick={this.showVersions}>
                      Version history…
                    </button>
                  ) : null}
                  <div className="display-inline-block align-button mbm">
                    {this.renderFooterStatus()}
                  </div>
                </div>
              </div>
            </div>
          </Collapsible>

        </FixedFooter>
      </div>
    );
  },

  renderFooterStatus: function() {
    if (this.isJustSaved() && !this.isSaving()) {
      return (
        <span className="fade-in type-green type-bold type-italic">All changes saved</span>
      );
    } else if (this.state.error === 'not_saved') {
      return (
        <span className="fade-in type-pink type-bold type-italic">
          <span style={{ height: 24 }} className="display-inline-block mrs align-b"><SVGWarning /></span>
          <span>Error saving changes — please try again</span>
        </span>
      );
    } else if (this.isModified()) {
      return (
        <ChangeSummary
          currentGroupVersion={this.getBehaviorGroup()}
          originalGroupVersion={this.props.group}
          isModified={this.editableIsModified}
        />
      );
    } else {
      return "";
    }
  },

  renderHiddenForms: function() {
    return (
      <div>
        <form ref="deleteBehaviorGroupForm" action={jsRoutes.controllers.ApplicationController.deleteBehaviorGroups().url} method="POST">
          <CsrfTokenHiddenInput value={this.props.csrfToken} />
          <input type="hidden" name="behaviorGroupIds[0]" value={this.getBehaviorGroup().id || ""} />
        </form>
      </div>
    );
  },

  getBehaviorHeadingText: function() {
    if (this.isDataTypeBehavior()) {
      if (this.isExistingBehavior()) {
        return "Edit data type";
      } else {
        return "New data type";
      }
    } else if (this.isExistingBehavior()) {
      return "Edit action";
    } else {
      return "New action";
    }
  },

  getBehaviorHeading: function() {
    return (
      <h5 className="type-blue-faded mbn">{this.getBehaviorHeadingText()}</h5>
    );
  },

  behaviorSwitcherIsVisible: function() {
    return this.state.behaviorSwitcherVisible;
  },

  mobileBehaviorSwitcherIsVisible: function() {
    return this.hasMobileLayout() && this.behaviorSwitcherIsVisible();
  },

  renderSwitcherToggle: function() {
    if ((!this.behaviorSwitcherIsVisible() || this.windowIsMobile()) && this.isExistingGroup()) {
      return (
        <div className="bg-white container container-wide type-weak border-bottom display-ellipsis display-limit-width">
          <button type="button" className="button-tab button-tab-subtle" onClick={this.toggleBehaviorSwitcher}>
            <span className="display-inline-block align-t mrm" style={{ height: "24px" }}>
              <SVGHamburger />
            </span>
            <h4 className="type-black display-inline-block align-m man">
              {this.getBehaviorGroup().getName()}
            </h4>
          </button>
        </div>
      );
    } else if (!this.isExistingGroup()) {
      return (
        <div className="bg-white container container-wide pvm border-bottom">
          <h4 className="man">New skill</h4>
        </div>
      );
    }
  },

  getEditorScrollPosition: function() {
    const selected = this.getSelected();
    return selected ? selected.editorScrollPosition : 0;
  },

  onSelect: function(groupId, id) {
    var newState = {
      animationDisabled: true,
      selectedId: id
    };
    if (this.windowIsMobile()) {
      newState.behaviorSwitcherVisible = false;
    }
    this.setState(newState, () => {
      if (groupId) {
        BrowserUtils.replaceURL(jsRoutes.controllers.BehaviorEditorController.edit(groupId, id).url);
      }
      var newScrollPosition = this.getEditorScrollPosition();
      window.scrollTo(window.scrollX, typeof(newScrollPosition) === 'number' ? newScrollPosition : 0);
      this.setState({
        animationDisabled: false
      });
    });
  },

  animationIsDisabled: function() {
    return this.state.animationDisabled;
  },

  addNewBehavior: function(isDataType, behaviorIdToClone) {
    const group = this.getBehaviorGroup();
    const url = jsRoutes.controllers.BehaviorEditorController.newUnsavedBehavior(isDataType, group.teamId, behaviorIdToClone).url;
    fetch(url, { credentials: 'same-origin' })
      .then((response) => response.json())
      .then((json) => {
        const newVersion = BehaviorVersion.fromJson(Object.assign({}, json, { groupId: group.id }));
        const groupWithNewBehavior = group.withNewBehaviorVersion(newVersion);
        this.updateGroupStateWith(groupWithNewBehavior, () => {
          this.onSelect(groupWithNewBehavior.id, newVersion.behaviorId);
        });
      });
  },

  addNewAction: function() {
    this.addNewBehavior(false);
  },

  addNewDataType: function() {
    this.addNewBehavior(true);
  },

  addNewLibrary: function(libraryIdToClone) {
    const group = this.getBehaviorGroup();
    const url = jsRoutes.controllers.BehaviorEditorController.newUnsavedLibrary(group.teamId, libraryIdToClone).url;
    fetch(url, { credentials: 'same-origin' })
      .then((response) => response.json())
      .then((json) => {
        const newVersion = new LibraryVersion(Object.assign({}, json, { groupId: group.id }));
        const groupWithNewLibrary = group.withNewLibraryVersion(newVersion);
        this.updateGroupStateWith(groupWithNewLibrary, () => {
          this.onSelect(groupWithNewLibrary.id, newVersion.libraryId);
        });
      });
  },

  renderBehaviorSwitcher: function() {
    return (
      <div ref="leftColumn"
        className={
          "column column-page-sidebar flex-column flex-column-left bg-white " +
          "border-right prn position-relative mobile-position-fixed-top-full mobile-position-z-front " +
          (this.behaviorSwitcherIsVisible() || this.hasMobileLayout()  ? "" : "display-none")
        }
      >
        <Collapsible revealWhen={this.behaviorSwitcherIsVisible()} animationDisabled={!this.hasMobileLayout()}>
          <Sticky ref="leftPanel" onGetCoordinates={this.getLeftPanelCoordinates} innerClassName="position-z-above" disabledWhen={this.hasMobileLayout()}>
            <div className="position-absolute position-top-right mtm mobile-mts mobile-mrs">
              <CollapseButton onClick={this.toggleBehaviorSwitcher} direction={this.windowIsMobile() ? "up" : "left"} />
            </div>
            <BehaviorSwitcher
              ref="behaviorSwitcher"
              actionBehaviors={this.getActionBehaviors()}
              dataTypeBehaviors={this.getDataTypeBehaviors()}
              libraries={this.getLibraries()}
              selectedId={this.getSelectedId()}
              groupId={this.getBehaviorGroup().id}
              groupName={this.getBehaviorGroup().getName()}
              groupDescription={this.getBehaviorGroup().description || ""}
              onBehaviorGroupNameChange={this.onBehaviorGroupNameChange}
              onBehaviorGroupDescriptionChange={this.onBehaviorGroupDescriptionChange}
              onSelect={this.onSelect}
              addNewAction={this.addNewAction}
              addNewDataType={this.addNewDataType}
              addNewLibrary={this.addNewLibrary}
              isModified={this.editableIsModified}
            />
          </Sticky>
        </Collapsible>
      </div>
    );
  },

  renderNameAndManagementActions: function() {
    return (
      <div className="container container-wide">
        <div className="columns columns-elastic mobile-columns-float">
          <div className="column column-shrink">
            <FormInput
              className="form-input-borderless form-input-l type-l type-semibold width-15 mobile-width-full"
              ref="input"
              value={this.getBehaviorName()}
              placeholder={this.isDataTypeBehavior() ? "Data type name" : "Action name (optional)"}
              onChange={this.updateName}
            />
          </div>
          <div className="column column-expand align-r align-b mobile-align-l mobile-mtl">
            {this.isExistingBehavior() ? (
              <span>
                <button type="button"
                  className="button-s mrs mbs"
                  onClick={this.cloneBehavior}>
                  {this.isDataTypeBehavior() ? "Clone data type…" : "Clone action…" }
                </button>
                <button type="button"
                  className="button-s mrs mbs"
                  onClick={this.confirmDeleteBehavior}>
                  {this.isDataTypeBehavior() ? "Delete data type…" : "Delete action…" }
                </button>
              </span>
            ) : null}
          </div>
        </div>
      </div>
    );
  },

  renderNormalBehavior: function() {
    return (

      <div>
                <div className="columns container container-wide">
                  <div className="column column-full mobile-column-full">
                    <FormInput
                      className="form-input-borderless form-input-m mbneg1"
                      placeholder="Action description (optional)"
                      onChange={this.updateDescription}
                      value={this.getEditableDescription()}
                    />
                  </div>
                </div>

                <hr className="mtn mbn thin bg-gray-light" />

                <TriggerConfiguration
                  isFinishedBehavior={this.isFinishedBehavior()}
                  triggers={this.getBehaviorTriggers()}
                  inputNames={this.getInputs().map((ea) => ea.name)}
                  onToggleHelp={this.toggleTriggerHelp}
                  helpVisible={this.props.activePanelName === 'helpForTriggerParameters'}
                  onTriggerAdd={this.addTrigger}
                  onTriggerChange={this.updateTriggerAtIndexWithTrigger}
                  onTriggerDelete={this.deleteTriggerAtIndex}
                  onTriggerDropdownToggle={this.toggleActiveDropdown}
                  onAddNewInput={this.addNewInput}
                  openDropdownName={this.getActiveDropdown()}
                />

                <UserInputConfiguration
                  ref="userInputConfiguration"
                  onInputChange={this.updateBehaviorInputAtIndexWith}
                  onInputDelete={this.deleteInputAtIndex}
                  onInputAdd={this.addNewInput}
                  onInputNameFocus={this.onInputNameFocus}
                  onInputNameBlur={this.onInputNameBlur}
                  onEnterKey={this.onInputEnterKey}
                  onConfigureType={this.onConfigureType}
                  userInputs={this.getInputs()}
                  paramTypes={this.getParamTypes()}
                  triggers={this.getBehaviorTriggers()}
                  isFinishedBehavior={this.isFinishedBehavior()}
                  behaviorHasCode={this.getSelectedBehavior().shouldRevealCodeEditor}
                  hasSharedAnswers={this.getOtherSavedInputsInGroup().length > 0}
                  otherBehaviorsInGroup={this.otherBehaviorsInGroup()}
                  onToggleSharedAnswer={this.toggleSharedAnswerInputSelector}
                  savedAnswers={this.props.savedAnswers}
                  onToggleSavedAnswer={this.toggleSavedAnswerEditor}
                  animationDisabled={this.animationIsDisabled()}
                />

                <Collapsible revealWhen={this.getSelectedBehavior().shouldRevealCodeEditor} animationDuration={0}>
                  <hr className="man thin bg-gray-light" />
                </Collapsible>

                <Collapsible revealWhen={!this.getSelectedBehavior().shouldRevealCodeEditor} animationDisabled={this.animationIsDisabled()}>
                  <div className="bg-blue-lighter border-top border-bottom border-blue pvl">
                    <div className="container container-wide">
                      <div className="columns columns-elastic narrow-columns-float">
                        <div className="column column-expand">
                          <p className="mbs">
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

                <Collapsible revealWhen={this.getSelectedBehavior().shouldRevealCodeEditor}
                  animationDuration={0.5}
                  animationDisabled={this.animationIsDisabled()}
                >
                  {this.renderCodeEditor({
                    sectionNumber: this.hasInputs() ? "3" : "2",
                    sectionHeading: "Run code",
                    codeEditorHelp: (
                      <CodeEditorHelp
                        isFinishedBehavior={this.isFinishedBehavior()}
                        functionBody={this.getFunctionBody()}
                        onToggleHelp={this.toggleBoilerplateHelp}
                        helpIsActive={this.props.activePanelName === 'helpForBoilerplateParameters'}
                        hasInputs={this.hasInputs()}
                      />
                    )
                  })}

                  <hr className="man thin bg-gray-light" />

                </Collapsible>

                <ResponseTemplateConfiguration
                  template={this.getBehaviorTemplate()}
                  onChangeTemplate={this.updateTemplate}
                  isFinishedBehavior={this.isFinishedBehavior()}
                  behaviorUsesCode={!!this.getSelectedBehavior().shouldRevealCodeEditor}
                  shouldForcePrivateResponse={this.shouldForcePrivateResponse()}
                  onChangeForcePrivateResponse={this.updateForcePrivateResponse}
                  onCursorChange={this.ensureCursorVisible}
                  onToggleHelp={this.toggleResponseTemplateHelp}
                  helpVisible={this.props.activePanelName === 'helpForResponseTemplate'}
                  sectionNumber={this.getResponseTemplateSectionNumber()}
                />

      </div>
    );
  },

  renderDataTypeBehavior: function() {
    return (
      <div className="pbxxxl">
        <hr className="mtl mbn thin bg-gray-light" />

        <DataTypeResultConfig
          usesSearch={this.hasInputNamed('searchQuery')}
          onChange={this.updateDataTypeResultConfig}
          isFinishedBehavior={this.isFinishedBehavior()}
        />

        <hr className="man thin bg-gray-light" />

          {this.renderCodeEditor({
            sectionNumber: "2",
            sectionHeading: "Run code to generate a list",
            codeEditorHelp: (
              <div className="mbxl">
                <DataTypeCodeEditorHelp
                  functionBody={this.getFunctionBody()}
                  usesSearch={this.hasInputNamed('searchQuery')}
                  isFinishedBehavior={this.isFinishedBehavior()}
                />
              </div>
            )
          })}
      </div>
    );
  },

  renderForBehaviorType: function() {
    if (this.isDataTypeBehavior()) {
      return this.renderDataTypeBehavior();
    } else {
      return this.renderNormalBehavior();
    }
  },

  renderLibrary: function() {
    return (
      <div className="pbxxxl">

        <div className="columns container container-wide">
          <div className="column column-full mobile-column-full">
            <FormInput
              className="form-input-borderless form-input-m mbneg1"
              placeholder="Library description (optional)"
              onChange={this.updateDescription}
              value={this.getEditableDescription()}
            />
          </div>
        </div>

        <hr className="man thin bg-gray-light" />

        {this.renderCodeEditor({
          systemParams: [],
          sectionNumber: "1",
          sectionHeading: "Write code to define a module",
          codeEditorHelp: (
            <LibraryCodeEditorHelp
              isFinished={this.isFinishedLibraryVersion()}
              functionBody={this.getFunctionBody()}
              onToggleHelp={this.toggleBoilerplateHelp}
              helpIsActive={this.props.activePanelName === 'helpForBoilerplateParameters'}
            />
          )
        })}
      </div>
    );
  },

  renderEditor: function() {
    if (this.getSelectedBehavior()) {
      return (
        <div>
          <div className="container container-wide mtl">
            {this.getBehaviorHeading()}
          </div>

          {this.renderNameAndManagementActions()}

          {this.renderForBehaviorType()}
        </div>
      );
    } else if (this.getSelectedLibrary()) {
      return (
        <div>
          {this.renderNameAndManagementActions()}
          {this.renderLibrary()}
        </div>
      );
    } else {
      return (
        <div>
          <BehaviorGroupEditor
            group={this.getBehaviorGroup()}
            isModified={this.isModified()}
            onBehaviorGroupNameChange={this.onBehaviorGroupNameChange}
            onBehaviorGroupDescriptionChange={this.onBehaviorGroupDescriptionChange}
            onBehaviorGroupIconChange={this.onBehaviorGroupIconChange}
            onDeleteClick={this.confirmDeleteBehaviorGroup}
          />
        </div>
      );
    }
  },

  render: function() {
    return (
      <div>
        <form action={this.getFormAction()} method="POST" ref="behaviorForm">
          <div className="columns flex-columns flex-columns-left mobile-flex-no-columns">
            {this.renderBehaviorSwitcher()}
            <div className="column column-page-main-wide flex-column flex-column-main">
              {this.renderSwitcherToggle()}

              {this.renderEditor()}
            </div>
          </div>

          {this.renderFooter()}

          {this.renderHiddenFormValues()}
        </form>

        {this.renderHiddenForms()}

      </div>
    );
  }
});

return PageWithPanels.with(BehaviorEditor);

});
