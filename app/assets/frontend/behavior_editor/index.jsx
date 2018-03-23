import * as React from 'react';
import APIConfigPanel from './api_config_panel';
import {AWSConfigRef} from '../models/aws';
import BehaviorGroup from '../models/behavior_group';
import BehaviorGroupVersionMetaData from '../models/behavior_group_version_meta_data';
import BehaviorGroupDetailsPanel from './behavior_group_details_panel';
import BehaviorGroupEditor from './behavior_group_editor';
import BehaviorVersion from '../models/behavior_version';
import BehaviorSwitcher from './behavior_switcher';
import BehaviorTester from './behavior_tester';
import DataTypeTester from './data_type_tester';
import BehaviorCodeHelp from './behavior_code_help';
import Button from '../form/button';
import CodeConfiguration from './code_configuration';
import ConfirmActionPanel from '../panels/confirm_action';
import CollapseButton from '../shared_ui/collapse_button';
import {DataRequest} from '../lib/data_request';
import DataTypeEditor from './data_type_editor';
import DataTypePromptHelp from './data_type_prompt_help';
import DataTypeSourceHelp from './data_type_source_help';
import DefaultStorageAdder from './default_storage_adder';
import DefaultStorageBrowser from './default_storage_browser';
import DevModeChannelsHelp from './dev_mode_channels_help';
import DynamicLabelButton from '../form/dynamic_label_button';
import EnvVariableAdder from '../settings/environment_variables/adder';
import EnvVariableSetter from '../settings/environment_variables/setter';
import HiddenJsonInput from './hidden_json_input';
import Input from '../models/input';
import Formatter from '../lib/formatter';
import ID from '../lib/id';
import NodeModuleVersion from '../models/node_module_version';
import FormInput from '../form/input';
import LibraryCodeHelp from './library_code_help';
import LibraryVersion from '../models/library_version';
import LinkedGithubRepo from '../models/linked_github_repo';
import ModalScrim from '../shared_ui/modal_scrim';
import Notifications from '../notifications/notifications';
import {OAuth2ApplicationRef} from '../models/oauth2';
import Page from '../shared_ui/page';
import ParamType from '../models/param_type';
import {RequiredAWSConfig} from '../models/aws';
import {RequiredOAuth2Application} from '../models/oauth2';
import ResponseTemplate from '../models/response_template';
import ResponseTemplateConfiguration from './response_template_configuration';
import ResponseTemplateHelp from './response_template_help';
import SavedAnswerEditor from './saved_answer_editor';
import SequentialName from '../lib/sequential_name';
import SharedAnswerInputSelector from './shared_answer_input_selector';
import {SimpleTokenApiRef} from '../models/simple_token';
import Sticky from '../shared_ui/sticky';
import SVGHamburger from '../svg/hamburger';
import Trigger from '../models/trigger';
import TriggerConfiguration from './trigger_configuration';
import TriggerHelp from './trigger_help';
import UniqueBy from '../lib/unique_by';
import UserInputConfiguration from './user_input_configuration';
import UserInputHelp from './user_input_help';
import VersionBrowser from './versions/version_browser';
import SVGWarning from '../svg/warning';
import Collapsible from '../shared_ui/collapsible';
import CsrfTokenHiddenInput from '../shared_ui/csrf_token_hidden_input';
import BrowserUtils from '../lib/browser_utils';
import Event from '../lib/event';
import ImmutableObjectUtils from '../lib/immutable_object_utils';
import debounce from 'javascript-debounce';
import Sort from '../lib/sort';
import 'codemirror/mode/markdown/markdown';
import DeploymentStatus from "./deployment_status";
import GithubRepoActions from "./versions/github_repo_actions";
import {MOBILE_MAX_WIDTH} from "../lib/constants";
import DataTypeDuplicateFieldsNotificationData from "../models/notifications/data_type_duplicate_fields_notification_data";
import DataTypeMissingFieldsNotificationData from "../models/notifications/data_type_missing_fields_notification_data";
import DataTypeNeedsConfigNotificationData from "../models/notifications/data_type_needs_config_notification_data";
import DataTypeUnnamedFieldsNotificationData from "../models/notifications/data_type_unnamed_fields_notification_data";
import DataTypeUnnamedNotificationData from "../models/notifications/data_type_unnamed_notification_data";
import EnvVarMissingNotificationData from "../models/notifications/env_var_missing_notification_data";
import RequiredAwsConfigNotificationData from "../models/notifications/required_aws_config_notification_data";
import OAuth2ConfigWithoutApplicationNotificationData from "../models/notifications/oauth2_config_without_application_notification_data";
import ServerDataWarningNotificationData from "../models/notifications/server_data_warning_notification_data";
import SkillDetailsWarningNotificationData from "../models/notifications/skill_details_warning_notification_data";
import UnknownParamInTemplateNotificationData from "../models/notifications/unknown_param_in_template_notification_data";

const BehaviorEditor = React.createClass({
  propTypes: Object.assign({}, Page.requiredPropTypes, {
    group: React.PropTypes.instanceOf(BehaviorGroup).isRequired,
    selectedId: React.PropTypes.string,
    csrfToken: React.PropTypes.string.isRequired,
    builtinParamTypes: React.PropTypes.arrayOf(React.PropTypes.instanceOf(ParamType)).isRequired,
    envVariables: React.PropTypes.arrayOf(React.PropTypes.object),
    awsConfigs: React.PropTypes.arrayOf(React.PropTypes.instanceOf(AWSConfigRef)),
    oauth2Applications: React.PropTypes.arrayOf(React.PropTypes.instanceOf(OAuth2ApplicationRef)),
    oauth2Apis: React.PropTypes.arrayOf(React.PropTypes.shape({
      apiId: React.PropTypes.string.isRequired,
      name: React.PropTypes.string.isRequired
    })),
    simpleTokenApis: React.PropTypes.arrayOf(React.PropTypes.instanceOf(SimpleTokenApiRef)),
    linkedOAuth2ApplicationIds: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
    linkedGithubRepo: React.PropTypes.instanceOf(LinkedGithubRepo),
    savedAnswers: React.PropTypes.arrayOf(
      React.PropTypes.shape({
        inputId: React.PropTypes.string.isRequired,
        userAnswerCount: React.PropTypes.number.isRequired,
        myValueString: React.PropTypes.string
      })
    ).isRequired,
    onSave: React.PropTypes.func.isRequired,
    onLinkGithubRepo: React.PropTypes.func.isRequired,
    onUpdateFromGithub: React.PropTypes.func.isRequired,
    onForgetSavedAnswerForInput: React.PropTypes.func.isRequired,
    onLoad: React.PropTypes.func,
    userId: React.PropTypes.string.isRequired,
    isAdmin: React.PropTypes.bool.isRequired,
    isLinkedToGithub: React.PropTypes.bool.isRequired,
    showVersions: React.PropTypes.bool,
    onDeploy: React.PropTypes.func.isRequired,
    lastDeployTimestamp: React.PropTypes.string,
    slackTeamId: React.PropTypes.string,
    botName: React.PropTypes.string.isRequired
  }),

  getDefaultProps: function() {
    return Page.requiredPropDefaults();
  },

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

  getAllAWSConfigs: function() {
    return this.props.awsConfigs || [];
  },

  getRequiredAWSConfigs: function() {
    return this.getBehaviorGroup().getRequiredAWSConfigs();
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
      .filter(ea => !!ea.config);
  },

  getSelectedApiConfigId: function() {
    return this.state ? this.state.selectedApiConfigId : undefined;
  },

  getSelectedApiConfig: function() {
    const selectedId = this.getSelectedApiConfigId();
    return this.getRequiredApiConfigWithId(selectedId);
  },

  getAllConfigs: function() {
    return this.getAllAWSConfigs()
      .concat(this.getAllOAuth2Applications())
      .concat(this.getAllSimpleTokenApis());
  },

  getAllRequiredApiConfigs: function() {
    return this.getRequiredAWSConfigs()
      .concat(this.getRequiredOAuth2ApiConfigs())
        .concat(this.getRequiredSimpleTokenApis());
  },

  getRequiredApiConfigWithId: function(id) {
    return this.getAllRequiredApiConfigs().find(ea => ea.id === id);
  },

  getApiConfigsForSelected: function() {
    const selected = this.getSelectedApiConfig();
    return selected ? selected.getAllConfigsFrom(this) : this.getAllConfigs();
  },

  onAddNewConfig(required, callback) {
    required.onAddConfigFor(this)(required, callback);
    this.selectRequiredApiConfig(required);
  },

  onAddConfigForSelected: function() {
    const selected = this.getSelectedApiConfig();
    return selected ? selected.onAddConfigFor(this) : this.onAddNewConfig;
  },

  onAddNewConfigForSelected: function() {
    const selected = this.getSelectedApiConfig();
    return selected ? selected.onAddNewConfigFor(this) : undefined;
  },

  onRemoveNewConfig(required, callback) {
    required.onRemoveConfigFor(this)(required, callback);
  },

  onRemoveConfigForSelected: function() {
    const selected = this.getSelectedApiConfig();
    return selected ? selected.onRemoveConfigFor(this) : this.onRemoveNewConfig;
  },

  onUpdateNewConfig(required, callback) {
    required.onUpdateConfigFor(this)(required, callback);
  },

  onUpdateConfigForSelected: function() {
    const selected = this.getSelectedApiConfig();
    return selected ? selected.onUpdateConfigFor(this) : this.onUpdateNewConfig;
  },

  getApiLogoUrlForApi(api) {
    return api ? (api.logoImageUrl || api.iconImageUrl) : null;
  },

  getOAuth2LogoUrlForConfig: function(config) {
    const api = this.getOAuth2ApiWithId(config.apiId);
    return this.getApiLogoUrlForApi(api);
  },

  getSimpleTokenLogoUrlForConfig: function(config) {
    const api = this.getSimpleTokenApiWithId(config.apiId);
    return this.getApiLogoUrlForApi(api);
  },

  getApiLogoUrlForConfig: function(config) {
    return config.getApiLogoUrl(this);
  },

  getApiNameForConfig: function(config) {
    return config.getApiName(this);
  },

  getApiConfigName: function(config) {
    const apiName = this.getApiNameForConfig(config);
    const configName = config.configName();
    if (configName.toLowerCase().includes(apiName.toLowerCase())) {
      return configName;
    } else if (configName) {
      return `${apiName} — ${configName}`;
    } else {
      return apiName;
    }
  },

  getOAuth2ApiNameForConfig: function(config) {
    const api = this.getOAuth2ApiWithId(config.apiId);
    return api ? api.name : "";
  },

  getSimpleTokenNameForConfig: function(config) {
    const api = this.getSimpleTokenApiWithId(config.apiId);
    return api ? api.displayName : "";
  },

  getEditableName: function() {
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
    if (!template && !this.isDataTypeBehavior()) {
      return new ResponseTemplate();
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
    return BehaviorVersion.defaultActionProps().responseTemplate;
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
        .map((ea) => new EnvVarMissingNotificationData({
          environmentVariableName: ea.name,
          onClick: () => {
            this.showEnvVariableSetter(ea.name);
          }
        }));
    } else {
      return [];
    }
  },

  getRequiredAWSConfigsWithNoMatchingAWSConfig: function() {
    return this.getRequiredAWSConfigs().filter(ea => !ea.config);
  },

  buildAWSNotifications: function() {
    if (this.isConfiguringApi()) {
      return [];
    }
    return this.getRequiredAWSConfigsWithNoMatchingAWSConfig().map(ea => new RequiredAwsConfigNotificationData({
      name: ea.nameInCode,
      requiredAWSConfig: ea,
      existingAWSConfigs: this.getAllAWSConfigs(),
      onUpdateAWSConfig: this.onUpdateAWSConfig,
      onNewAWSConfig: this.onNewAWSConfig,
      onConfigClick: this.onApiConfigClick.bind(this, ea)
    }));
  },

  getOAuth2ApiWithId: function(apiId) {
    return this.props.oauth2Apis.find(ea => ea.apiId === apiId);
  },

  getSimpleTokenApiWithId: function(tokenId) {
    return this.props.simpleTokenApis.find(ea => ea.id === tokenId);
  },

  getRequiredOAuth2ApiConfigsWithNoApplication: function() {
    return this.getRequiredOAuth2ApiConfigs().filter(ea => !ea.config);
  },

  getOAuth2ApplicationsRequiringAuth: function() {
    return this.getApiApplications().filter(ea => {
      return !this.props.linkedOAuth2ApplicationIds.includes(ea.config.id);
    });
  },

  isConfiguringApi: function() {
    return this.props.activePanelName === "configureApi";
  },

  buildOAuthApplicationNotifications: function() {
    if (this.isConfiguringApi()) {
      return [];
    }
    return this.getRequiredOAuth2ApiConfigsWithNoApplication().map(ea => new OAuth2ConfigWithoutApplicationNotificationData({
      name: this.getOAuth2ApiWithId(ea.apiId).name,
      requiredApiConfig: ea,
      existingOAuth2Applications: this.getAllOAuth2Applications(),
      onUpdateOAuth2Application: this.onUpdateOAuth2Application,
      onNewOAuth2Application: this.onNewOAuth2Application,
      onConfigClick: this.onApiConfigClick.bind(this, ea)
    }));
  },

  getParamTypesNeedingConfiguration: function() {
    const paramTypes = Array.from(new Set(this.getInputs().map(ea => ea.paramType)));
    return paramTypes.filter(ea => ea.needsConfig);
  },

  buildDataTypeNotifications: function() {
    const needsConfig = this.getParamTypesNeedingConfiguration().map(ea => {
      const behaviorVersion = this.getBehaviorGroup().behaviorVersions.find(bv => bv.id === ea.id);
      const behaviorId = behaviorVersion ? behaviorVersion.behaviorId : null;
      return new DataTypeNeedsConfigNotificationData({
        name: ea.name,
        onClick: () => this.onSelect(this.getBehaviorGroup().id, behaviorId)
      });
    });

    const dataTypes = this.getDataTypeBehaviors();

    const unnamedDataTypes = dataTypes
      .filter((ea) => !ea.getName().trim())
      .map((ea) => {
        return new DataTypeUnnamedNotificationData({
          onClick: () => {
            this.onSelect(this.getBehaviorGroup().id, ea.behaviorId, () => {
              if (this.editableNameInput) {
                this.editableNameInput.focus();
              }
            });
          }
        });
      });

    const missingFields = dataTypes
      .filter((ea) => ea.getDataTypeConfig().isMissingFields())
      .map((ea) => {
        return new DataTypeMissingFieldsNotificationData({
          name: ea.getName(),
          onClick: () => {
            this.onSelect(this.getBehaviorGroup().id, ea.behaviorId, () => {
              if (this.dataTypeEditor) {
                this.dataTypeEditor.addNewDataTypeField();
              }
            });
          }
        });
      });

    const unnamedFields = dataTypes
      .filter((dataType) => dataType.requiresFields() && dataType.getDataTypeFields().some((field) => !field.name))
      .map((ea) => {
        return new DataTypeUnnamedFieldsNotificationData({
          name: ea.getName(),
          onClick: () => {
            this.onSelect(this.getBehaviorGroup().id, ea.behaviorId, () => {
              if (this.dataTypeEditor) {
                this.dataTypeEditor.focusOnFirstBlankField();
              }
            });
          }
        });
      });

    const duplicateFields = dataTypes
      .filter((dataType) => {
        if (dataType.requiresFields()) {
          const names = dataType.getDataTypeFields().map((ea) => ea.name).filter((ea) => ea.length > 0);
          const uniqueNames = new Set(names);
          return uniqueNames.size < names.length;
        } else {
          return false;
        }
      })
      .map((ea) => {
        return new DataTypeDuplicateFieldsNotificationData({
          name: ea.getName(),
          onClick: () => {
            this.onSelect(this.getBehaviorGroup().id, ea.behaviorId, () => {
              if (this.dataTypeEditor) {
                this.dataTypeEditor.focusOnDuplicateField();
              }
            });
          }
        });
      });

    return [].concat(needsConfig, unnamedDataTypes, missingFields, unnamedFields, duplicateFields);
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
      return unknownTemplateParams.map((paramName) => new UnknownParamInTemplateNotificationData({
        name: paramName
      }));
    } else {
      return [];
    }
  },

  buildServerNotifications: function() {
    if (!this.state) return [];
    const notifications = [];
    if (this.state.newerVersionOnServer) {
      notifications.push(new ServerDataWarningNotificationData({
        type: "newer_version",
        newerVersion: this.state.newerVersionOnServer,
        currentUserId: this.props.userId,
        onClick: () => {
          window.location.reload();
        }
      }));
    }
    if (this.state.errorReachingServer) {
      notifications.push(new ServerDataWarningNotificationData({
        type: "network_error",
        error: this.state.errorReachingServer
      }));
    }
    return notifications;
  },

  buildSkillDetailsNotifications: function() {
    if (this.isExistingGroup() && !this.getBehaviorGroup().name) {
      return [new SkillDetailsWarningNotificationData({
        type: "no_skill_name",
        onClick: this.toggleRequestSkillDetails
      })];
    } else {
      return [];
    }
  },

  buildNotifications: function() {
    return [].concat(
      this.buildEnvVarNotifications(),
      this.buildAWSNotifications(),
      this.buildOAuthApplicationNotifications(),
      this.buildDataTypeNotifications(),
      this.buildTemplateNotifications(),
      this.buildServerNotifications(),
      this.buildSkillDetailsNotifications()
    );
  },

  getNotifications: function() {
    return this.state.notifications;
  },

  getVersions: function() {
    return this.state.versions;
  },

  getParamTypes: function() {
    var customTypes = Sort.arrayAlphabeticalBy(this.getBehaviorGroup().getCustomParamTypes(), (ea) => ea.name);
    return this.props.builtinParamTypes.concat(customTypes);
  },

  getParamTypesForDataTypes: function() {
    // TODO: use getParamTypes instead if we want to support custom data types
    return this.props.builtinParamTypes;
  },

  /* Setters/togglers */

  setBehaviorInputs: function(newBehaviorInputs, callback) {
    const newGroup = this.getBehaviorGroup().copyWithInputsForBehaviorVersion(newBehaviorInputs, this.getSelectedBehavior());
    this.updateGroupStateWith(newGroup, callback);
  },

  addInput: function(input, callback) {
    const newInputs = this.getInputs().concat([input]);
    this.setBehaviorInputs(newInputs, callback);
  },

  addNewInput: function(optionalNewName, callback) {
    const newName = optionalNewName || SequentialName.nextFor(this.getInputs(), (ea) => ea.name, "userInput");
    const newInput = Input.fromProps({
      name: newName,
      question: "",
      paramType: this.props.builtinParamTypes.find((ea) => ea.id === "Text"),
      isSavedForTeam: false,
      isSavedForUser: false,
      inputId: ID.next(),
      exportId: null
    });
    this.addInput(newInput, callback);
  },

  addTrigger: function(callback) {
    const newTrigger = new Trigger();
    this.setEditableProp('triggers', this.getBehaviorTriggers().concat(newTrigger), callback);
  },

  cloneEditable: function() {
    const editable = this.getSelected();
    if (editable) {
      if (editable.isBehaviorVersion()) {
        this.addNewBehavior(editable.isDataType(), editable.behaviorId);
      } else {
        this.cloneLibrary(editable.libraryId);
      }
    }
  },

  confirmDeleteEditable: function() {
    this.toggleActivePanel('confirmDeleteEditable', true);
  },

  confirmDeleteBehaviorGroup: function() {
    this.toggleActivePanel('confirmDeleteBehaviorGroup', true);
  },

  toggleConfirmUndo: function() {
    this.toggleActivePanel('confirmUndo', true);
  },

  confirmRevert: function(newBehaviorGroup, newBehaviorGroupTitle) {
    this.setState({
      revertToVersion: newBehaviorGroup,
      revertToVersionTitle: newBehaviorGroupTitle
    }, this.toggleConfirmRevert);
  },

  doRevert: function() {
    if (this.state.revertToVersion) {
      const newGroup = this.state.revertToVersion;
      this.setState({
        revertToVersion: null,
        revertToVersionTitle: null
      }, () => {
        this.onReplaceBehaviorGroup(newGroup);
      });
    }
  },

  toggleConfirmRevert: function() {
    this.toggleActivePanel('confirmRevert', true, () => {
      if (this.props.activePanelName !== 'confirmRevert') {
        this.setState({
          revertToVersion: null,
          revertToVersionTitle: null
        });
      }
    });
  },

  deleteEditable: function() {
    this.props.onClearActivePanel();
    const group = this.getBehaviorGroup();
    const updatedGroup = group.clone({
      behaviorVersions: group.behaviorVersions.filter(ea => ea.behaviorId !== this.getSelectedId()),
      libraryVersions: group.libraryVersions.filter(ea => ea.libraryId !== this.getSelectedId())
    });
    this.updateGroupStateWith(updatedGroup);
  },

  deleteBehaviorGroup: function() {
    if (this.deleteBehaviorGroupForm) {
      this.deleteBehaviorGroupForm.submit();
    }
  },

  deleteInputAtIndex: function(index) {
    this.setEditableProp('inputIds', ImmutableObjectUtils.arrayRemoveElementAtIndex(this.getInputIds(), index));
  },

  deleteAllInputs: function(callback) {
    this.setEditableProp('inputIds', [], callback);
  },

  deleteTriggerAtIndex: function(index) {
    var triggers = ImmutableObjectUtils.arrayRemoveElementAtIndex(this.getBehaviorTriggers(), index);
    this.setEditableProp('triggers', triggers);
  },

  getLeftPanelCoordinates: function() {
    var headerHeight = this.getHeaderHeight();
    var footerHeight = this.props.activePanelIsModal ? 0 : this.props.footerHeight;
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
        behaviorSwitcherVisible: !this.windowIsMobile(),
        hasMobileLayout: this.windowIsMobile()
      });
    }
  },

  layoutDidUpdate: function() {
    if (this.leftPanel) {
      this.leftPanel.resetCoordinates();
    }
  },

  getHeaderHeight: function() {
    var mainHeader = document.getElementById('main-header');
    return mainHeader ? mainHeader.offsetHeight : 0;
  },

  updateBehaviorScrollPosition: function() {
    if (this.getSelected()) {
      this.setEditableProp('editorScrollPosition', window.scrollY);
    }
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
          versions: versions,
          versionsLoadStatus: 'loaded'
        });
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
    if (this.getActiveDropdown()) {
      this.hideActiveDropdown();
    }
  },

  onDocumentKeyDown: function(event) {
    if (Event.keyPressWasEsc(event)) {
      this.handleEscKey(event);
    } else if (Event.keyPressWasSaveShortcut(event)) {
      event.preventDefault();
      if (this.isModified()) {
        this.onSaveBehaviorGroup();
      }
    }
  },

  onSaveError: function(error) {
    this.props.onClearActivePanel();
    this.setState({
      error: error || "not_saved"
    });
  },

  onDeployError: function(error, callback) {
    this.setState({
      error: (error ? error.body : null) || "not_deployed"
    }, callback);
    this.resetNotificationsImmediately();
  },

  deploy: function(callback) {
    this.setState({ error: null });
    DataRequest.jsonPost(
      jsRoutes.controllers.BehaviorEditorController.deploy().url,
      { behaviorGroupId: this.getBehaviorGroup().id },
      this.props.csrfToken
    )
      .then((json) => {
        if (json.id) {
          this.props.onDeploy(json, callback);
        } else {
          this.onDeployError(null, callback);
        }
      })
      .catch(error => {
        this.onDeployError(error, callback);
      });
  },

  updateNodeModules: function(optionalCallback) {
    this.setState({ error: null });
    this.toggleActivePanel('saving', true);
    DataRequest.jsonPost(
      jsRoutes.controllers.BehaviorEditorController.updateNodeModules().url,
      { behaviorGroupId: this.getBehaviorGroup().id },
      this.props.csrfToken
    )
      .then((json) => {
        if (json.id) {
          const newProps = {
            group: BehaviorGroup.fromJson(json),
            onLoad: optionalCallback
          };
          this.onSave(newProps);
        } else {
          this.onSaveError();
        }
      })
      .catch((error) => {
        this.onSaveError(error);
      });
  },

  backgroundSave: function(optionalCallback) {
    var formData = new FormData(this.behaviorForm);
    this.setState({
      newerVersionOnServer: null,
      errorReachingServer: null
    }, () => this.doBackgroundSave(formData, optionalCallback));
  },

  doBackgroundSave: function(formData, optionalCallback) {
    fetch(this.getFormAction(), {
      credentials: 'same-origin',
      method: 'POST',
      headers: {
        'Accept': 'application/json',
        'Csrf-Token': this.props.csrfToken,
        'x-requested-with': 'XMLHttpRequest'
      },
      body: formData
    }).then((response) => response.json())
      .then((json) => {
        if (json.id) {
          const group = this.getBehaviorGroup();
          const teamId = group.teamId;
          const groupId = json.id;
          if (this.state.shouldRedirectToAddNewOAuth2App) {
            const config = this.state.requiredOAuth2ApiConfig;
            window.location.href = jsRoutes.controllers.web.settings.OAuth2ApplicationController.add(teamId, groupId, this.getSelectedId(), config.nameInCode).url;
          } else if (this.state.shouldRedirectToAddNewAWSConfig) {
            const config = this.state.requiredAWSConfig;
            window.location.href = jsRoutes.controllers.web.settings.AWSConfigController.add(teamId, groupId, this.getSelectedId(), config.nameInCode).url;
          } else {
            const newProps = {
              group: BehaviorGroup.fromJson(json),
              onLoad: optionalCallback
            };
            this.onSave(newProps);
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

  onSaveClick: function() {
    this.onSaveBehaviorGroup();
  },

  onSaveBehaviorGroup: function(optionalCallback) {
    this.setState({ error: null }, () => {
      this.toggleActivePanel('saving', true, () => {
        this.backgroundSave(optionalCallback);
      });
    });
  },

  onReplaceBehaviorGroup: function(newBehaviorGroup) {
    const newState = {
      group: newBehaviorGroup
    };
    if (!newBehaviorGroup.hasBehaviorVersionWithId(this.getSelectedId())) {
      newState.selectedId = null;
    }
    this.setState(newState, this.onSaveBehaviorGroup);
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

  setConfigProps: function(props, callback) {
    const existingGroup = this.getBehaviorGroup();
    const existingSelected = this.getSelectedFor(existingGroup, this.getSelectedId());
    const existingConfig = existingSelected.config;
    this.setEditableProp("config", existingConfig.clone(props), callback);
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

    this.setState({
      group: timestampedGroup,
      selectedId: selectedIdAfter
    }, () => {
      if (callback) {
        callback();
      }
      this.resetNotificationsEventually();
    });
  },

  setConfigProperty: function(property, value, callback) {
    const newProps = {};
    newProps[property] = value;
    const newConfig = this.getBehaviorConfig().clone(newProps);
    this.setEditableProp('config', newConfig, callback);
  },

  showEnvVariableAdder: function(prompt) {
    this.setState({
      envVariableAdderPrompt: prompt
    }, function () {
      this.toggleActivePanel('envVariableAdder', true);
    });
  },

  showEnvVariableSetter: function(nameToFocus) {
    this.toggleActivePanel('envVariableSetter', true, () => {
      if (nameToFocus && this.envVariableSetterPanel) {
        this.envVariableSetterPanel.focusOnVarName(nameToFocus);
      }
    });
  },

  showVersions: function() {
    if (!this.versionsMaybeLoaded()) {
      this.loadVersions();
    }
    this.toggleActivePanel('versionBrowser', false, this.updateVersionBrowserOpenState);
  },

  updateVersionBrowserOpenState: function() {
    this.setState({
      versionBrowserOpen: this.props.activePanelName === 'versionBrowser'
    }, () => {
      if (this.state.versionBrowserOpen) {
        BrowserUtils.replaceQueryParam("showVersions", "true");
      } else {
        BrowserUtils.removeQueryParam("showVersions");
      }
    });
  },

  toggleActiveDropdown: function(name) {
    var alreadyOpen = this.getActiveDropdown() === name;
    this.setState({
      activeDropdown: alreadyOpen ? null : { name: name }
    });
  },

  toggleActivePanel: function(name, beModal, optionalCallback) {
    this.props.onToggleActivePanel(name, beModal, optionalCallback);
  },

  toggleChangeGithubRepo: function() {
    this.setState({
      isModifyingGithubRepo: !this.state.isModifyingGithubRepo
    });
  },

  toggleSharedAnswerInputSelector: function() {
    this.toggleActivePanel('sharedAnswerInputSelector', true);
  },

  toggleRequestSkillDetails: function() {
    this.toggleActivePanel('requestBehaviorGroupDetails', true);
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
    this.toggleActivePanel(ref, true);
  },

  toggleBehaviorCodeHelp: function() {
    this.toggleActivePanel('helpForBehaviorCode');
  },

  toggleCodeEditorLineWrapping: function() {
    this.setState({
      codeEditorUseLineWrapping: !this.state.codeEditorUseLineWrapping
    });
  },

  toggleResponseTemplateHelp: function() {
    this.toggleActivePanel('helpForResponseTemplate');
  },

  toggleDevModeChannelsHelp: function() {
    this.toggleActivePanel('helpForDevModeChannels');
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

  toggleUserInputHelp: function() {
    this.toggleActivePanel('helpForUserInput');
  },

  toggleTriggerHelp: function() {
    this.toggleActivePanel('helpForTriggerParameters');
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
        if (this.envVariableAdderPanel) {
          this.envVariableAdderPanel.onSaveError();
        }
      }
    });
  },

  updateDescription: function(newDescription) {
    this.setEditableProp('description', newDescription);
  },

  updateName: function(newName) {
    const normalizedName = this.isDataTypeBehavior() ? Formatter.formatDataTypeName(newName) : newName;
    this.setEditableProp('name', normalizedName);
  },

  updateEnvVariables: function(envVars, options) {
    var url = jsRoutes.controllers.web.settings.EnvironmentVariablesController.submit().url;
    var data = {
      teamId: this.getBehaviorGroup().teamId,
      variables: envVars
    };
    fetch(url, this.jsonPostOptions({ teamId: this.getBehaviorGroup().teamId, dataJson: JSON.stringify(data) }))
      .then((response) => response.json())
      .then((json) => {
        this.props.onClearActivePanel();
        if (this.envVariableAdderPanel) {
          this.envVariableAdderPanel.reset();
        }
        this.setState({
          envVariables: json.variables
        }, () => {
          this.resetNotificationsImmediately();
          if (this.envVariableSetterPanel) {
            this.envVariableSetterPanel.reset();
          }
          if (options && options.saveCallback) {
            options.saveCallback();
          }
        });
      }).catch(() => {
        if (this.envVariableSetterPanel) {
          this.envVariableSetterPanel.onSaveError();
        }
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

  moveBehaviorInputAtIndex: function(index, newIndex) {
    const oldInputs = this.getInputs();
    this.setBehaviorInputs(ImmutableObjectUtils.arrayMoveElement(oldInputs, index, newIndex));
  },

  updateForcePrivateResponse: function(newValue) {
    this.setConfigProperty('forcePrivateResponse', newValue);
  },

  updateTemplate: function(newTemplateString) {
    this.setEditableProp('responseTemplate', this.getBehaviorTemplate().clone({ text: newTemplateString }));
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
      this.resetNotificationsEventually();
    });
  },

  /* Booleans */

  isTestable: function() {
    return Boolean(this.getSelectedBehavior() && this.getSelectedBehavior().usesCode());
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

  getNodeModuleVersions: function() {
    return this.state.nodeModuleVersions || [];
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

  isExisting: function() {
    return !!this.getSelected() && !this.getSelected().isNew;
  },

  isExistingGroup: function() {
    return this.getBehaviorGroup().isExisting();
  },

  isFinishedBehavior: function() {
    var originalSelected = this.getOriginalSelected();
    return !!(originalSelected && !originalSelected.isNew &&
      (originalSelected.functionBody || originalSelected.responseTemplate.text));
  },

  isModified: function() {
    var currentMatchesInitial = this.props.group.isIdenticalTo(this.getBehaviorGroup());
    return !currentMatchesInitial;
  },

  editableIsModified: function(current) {
    var original = this.props.group.getEditables().find((ea) => ea.getPersistentId() === current.getPersistentId());
    return !(original && current.isIdenticalToVersion(original));
  },

  isSaving: function() {
    return this.props.activePanelName === 'saving';
  },

  versionsMaybeLoaded: function() {
    return this.state.versionsLoadStatus === 'loading' || this.state.versionsLoadStatus === 'loaded';
  },

  /* Interaction and event handling */

  ensureCursorVisible: function(editor) {
    const height = this.props.footerHeight;
    if (!height) {
      return;
    }
    var cursorBottom = editor.cursorCoords(false).bottom;
    BrowserUtils.ensureYPosInView(cursorBottom, height);
  },

  onAddNewEnvVariable: function() {
    this.showEnvVariableAdder();
  },

  onAddAWSConfig: function(toAdd, callback) {
    const existing = this.getRequiredAWSConfigs();
    const newConfigs = existing.concat([toAdd]);
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ requiredAWSConfigs: newConfigs }), callback);
  },

  onRemoveAWSConfig: function(config, callback) {
    const existing = this.getRequiredAWSConfigs();
    const newConfigs = existing.filter(ea => {
      return ea.id !== config.id;
    });
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ requiredAWSConfigs: newConfigs }), () => {
      this.props.onClearActivePanel();
      if (callback) {
        callback();
      }
    });
  },

  onUpdateAWSConfig: function(config, callback) {
    const configs = this.getRequiredAWSConfigs().slice();
    const indexToReplace = configs.findIndex(ea => ea.id === config.id);
    configs[indexToReplace] = config;
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ requiredAWSConfigs: configs }), callback);
  },

  addNewAWSConfig: function(required) {
    const requiredToUse = required || RequiredAWSConfig.fromProps({
      id: ID.next(),
      exportId: null,
      apiId: 'aws',
      nameInCode: "",
      config: null
    });
    this.onNewAWSConfig(requiredToUse);
  },

  onAddOAuth2Application: function(toAdd, callback) {
    const existing = this.getRequiredOAuth2ApiConfigs();
    const newApplications = existing.concat([toAdd]);
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ requiredOAuth2ApiConfigs: newApplications }), callback);
  },

  onRemoveOAuth2Application: function(toRemove, callback) {
    const existing = this.getRequiredOAuth2ApiConfigs();
    const newApplications = existing.filter(function(ea) {
      return ea.id !== toRemove.id;
    });
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ requiredOAuth2ApiConfigs: newApplications }), () => {
      this.props.onClearActivePanel();
      if (callback) {
        callback();
      }
    });
  },

  onUpdateOAuth2Application: function(config, callback) {
    const configs = this.getRequiredOAuth2ApiConfigs().slice();
    const indexToReplace = configs.findIndex(ea => ea.id === config.id);
    configs[indexToReplace] = config;
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ requiredOAuth2ApiConfigs: configs }), callback);
  },

  addNewOAuth2Application: function(required) {
    const requiredToUse = required || RequiredOAuth2Application.fromProps({
      id: ID.next(),
      exportId: null,
      apiId: "",
      nameInCode: "",
      config: null,
      recommendedScope: ""
    });
    this.onNewOAuth2Application(requiredToUse);
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

  onUpdateSimpleTokenApi: function(config, callback) {
    const configs = this.getRequiredSimpleTokenApis().slice();
    const indexToReplace = configs.findIndex(ea => ea.id === config.id);
    configs[indexToReplace] = config;
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ requiredSimpleTokenApis: configs }), callback);
  },

  onNewAWSConfig: function(requiredAWSConfig) {
    this.setState({
      shouldRedirectToAddNewAWSConfig: true,
      requiredAWSConfig: requiredAWSConfig
    }, this.onSaveBehaviorGroup);
  },

  onNewOAuth2Application: function(requiredOAuth2ApiConfig) {
    this.setState({
      shouldRedirectToAddNewOAuth2App: true,
      requiredOAuth2ApiConfig: requiredOAuth2ApiConfig
    }, this.onSaveBehaviorGroup);
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

  onSave: function(newProps) {
    this.props.onSave(newProps);
    this.loadNodeModuleVersions();
  },

  resetNotificationsImmediately: function() {
    this.setState({
      notifications: this.buildNotifications()
    });
  },

  loadNodeModuleVersions: function() {
    if (this.isExistingGroup()) {
      DataRequest.jsonGet(jsRoutes.controllers.BehaviorEditorController.nodeModuleVersionsFor(this.getBehaviorGroup().id).url)
        .then(json => {
          this.setState({
            nodeModuleVersions: NodeModuleVersion.allFromJson(json)
          });
        });
    }
  },

  resetNotificationsEventually: debounce(function() {
    this.resetNotificationsImmediately();
  }, 500),

    /* Component API methods */

  componentWillMount() {
    if (!this.isExistingGroup()) {
      const actions = this.getActionBehaviors();
      const updatedGroup = actions.reduce((group, action, index) => {
        if (action.isEmpty()) {
          return action.buildUpdatedGroupFor(group, BehaviorVersion.defaultActionProps(`action${index + 1}`));
        } else {
          return group;
        }
      }, this.getBehaviorGroup());
      this.updateGroupStateWith(updatedGroup);
      this.toggleRequestSkillDetails();
    }
  },

  componentDidMount: function() {
    window.document.addEventListener('click', this.onDocumentClick, false);
    window.document.addEventListener('keydown', this.onDocumentKeyDown, false);
    window.addEventListener('resize', this.checkMobileLayout, false);
    window.addEventListener('scroll', debounce(this.updateBehaviorScrollPosition, 500), false);
    window.addEventListener('focus', this.checkForUpdates, false);
    this.checkForUpdatesLater();
    this.loadNodeModuleVersions();
    if (this.props.showVersions) {
      this.showVersions();
    }
    this.renderNavItems();
    this.renderNavActions();
  },

  componentDidUpdate: function() {
    this.renderNavItems();
    this.renderNavActions();
  },

  checkForUpdates: function() {
    if (document.hasFocus() && this.isExistingGroup() && !this.isSaving()) {
      DataRequest.jsonGet(jsRoutes.controllers.BehaviorEditorController.metaData(this.getBehaviorGroup().id).url)
        .then((json) => {
          if (!json.createdAt) {
            throw new Error("Invalid response");
          }
          const serverDate = new Date(json.createdAt);
          const savedDate = new Date(this.props.group.createdAt);
          const isNewerVersion = serverDate > savedDate;
          const wasOldError = this.state.errorReachingServer;
          if (this.state.newerVersionOnServer || isNewerVersion || wasOldError) {
            this.setState({
              newerVersionOnServer: isNewerVersion ? BehaviorGroupVersionMetaData.fromJson(json) : null,
              errorReachingServer: null
            }, this.resetNotificationsEventually);
          }
          this.checkForUpdatesLater();
        })
        .catch((err) => {
          this.setState({
            errorReachingServer: err
          }, this.resetNotificationsEventually);
          this.checkForUpdatesLater();
        });
    } else {
      this.checkForUpdatesLater();
    }
  },

  checkForUpdatesLater: function() {
    setTimeout(this.checkForUpdates, 30000);
  },

  getInitialEnvVariables: function() {
    return Sort.arrayAlphabeticalBy(this.props.envVariables || [], (variable) => variable.name);
  },

  getInitialState: function() {
    return {
      group: this.props.group,
      selectedId: this.props.selectedId,
      activeDropdown: null,
      codeEditorUseLineWrapping: false,
      envVariables: this.getInitialEnvVariables(),
      notifications: this.buildNotifications(),
      versions: [],
      versionsLoadStatus: null,
      onNextNewEnvVar: null,
      envVariableAdderPrompt: null,
      redirectValue: "",
      requiredAWSConfig: null,
      shouldRedirectToAddNewAWSConfig: false,
      requiredOAuth2ApiConfig: null,
      shouldRedirectToAddNewOAuth2App: false,
      paramNameToSync: null,
      error: null,
      selectedSavedAnswerInputId: null,
      behaviorSwitcherVisible: !this.windowIsMobile(),
      hasMobileLayout: this.windowIsMobile(),
      animationDisabled: false,
      lastSavedDataStorageItem: null,
      nodeModuleVersions: [],
      selectedApiConfigId: null,
      newerVersionOnServer: null,
      errorReachingServer: null,
      versionBrowserOpen: false,
      revertToVersion: null,
      revertToVersionTitle: null,
      isModifyingGithubRepo: false,
      windowDimensions: {
        width: window.innerWidth,
        height: window.innerHeight
      }
    };
  },

  componentWillReceiveProps: function(nextProps) {
    if (nextProps.group !== this.props.group) {
      var newGroup = nextProps.group;
      var newState = {
        group: newGroup,
        versions: [],
        versionsLoadStatus: null,
        error: null
      };
      this.props.onClearActivePanel();
      this.setState(newState, this.resetNotificationsImmediately);
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

  renderCodeEditor: function(codeConfigProps) {
    return (
      <CodeConfiguration
        sectionNumber={codeConfigProps.sectionNumber}
        codeHelpPanelName={codeConfigProps.codeHelpPanelName}

        activePanelName={this.props.activePanelName}
        activeDropdownName={this.getActiveDropdown()}
        onToggleActiveDropdown={this.toggleActiveDropdown}
        onToggleActivePanel={this.toggleActivePanel}
        animationIsDisabled={this.animationIsDisabled()}

        behaviorConfig={this.getBehaviorConfig()}

        inputs={this.getInputs()}
        systemParams={codeConfigProps.systemParams || this.getSystemParams()}
        requiredAWSConfigs={this.getRequiredAWSConfigs()}
        apiApplications={this.getApiApplications()}

        functionBody={this.getFunctionBody()}
        onChangeFunctionBody={this.updateCode}
        onCursorChange={this.ensureCursorVisible}
        useLineWrapping={this.state.codeEditorUseLineWrapping}
        onToggleCodeEditorLineWrapping={this.toggleCodeEditorLineWrapping}

        envVariableNames={this.getEnvVariableNames()}
        functionExecutesImmediately={codeConfigProps.functionExecutesImmediately || false}
      />
    );
  },

  confirmDeleteText: function() {
    const selected = this.getSelected();
    return selected ? selected.confirmDeleteText() : "";
  },

  confirmRevertText: function() {
    const versionText = this.state.revertToVersion && this.state.revertToVersionTitle ? (
      <p>
        <span>Are you sure you want to switch to the </span>
        <span>{this.state.revertToVersionTitle}?</span>
      </p>
    ) : (
      <p>Are you sure you want to switch versions?</p>
    );
    return (
      <div>
        {versionText}
        {this.isModified() ? (
          <p>The changes you’ve made since last saving will be lost.</p>
        ) : (
          <p>The current version will be replaced, but it will still be available later if you need it.</p>
        )}
      </div>
    );
  },

  toggleApiAdderDropdown: function() {
    this.toggleActiveDropdown("apiConfigAdderDropdown");
  },

  renderFooter: function() {
    const footerClassName = this.mobileBehaviorSwitcherIsVisible() ? "mobile-position-behind-scrim" : "";
    return this.props.onRenderFooter((
      <div>
          <ModalScrim isActive={this.mobileBehaviorSwitcherIsVisible()} />
          {this.isDataTypeBehavior() ? (
            <div>
              <Collapsible ref={(el) => this.props.onRenderPanel("addDataStorageItems", el)} revealWhen={this.props.activePanelName === 'addDataStorageItems'} onChange={this.layoutDidUpdate}>
                <DefaultStorageAdder
                  csrfToken={this.props.csrfToken}
                  behaviorVersion={this.getSelectedBehavior()}
                  onCancelClick={this.props.onClearActivePanel}
                />
              </Collapsible>

              <Collapsible ref={(el) => this.props.onRenderPanel("browseDataStorage", el)} revealWhen={this.props.activePanelName === 'browseDataStorage'} onChange={this.layoutDidUpdate}>
                <DefaultStorageBrowser
                  csrfToken={this.props.csrfToken}
                  behaviorVersion={this.getSelectedBehavior()}
                  behaviorGroupId={this.getBehaviorGroup().id}
                  onCancelClick={this.props.onClearActivePanel}
                  isVisible={this.props.activePanelName === 'browseDataStorage'}
                />
              </Collapsible>
            </div>
          ) : null}

          <Collapsible ref={(el) => this.props.onRenderPanel("requestBehaviorGroupDetails", el)} revealWhen={this.props.activePanelName === 'requestBehaviorGroupDetails'}>
            <BehaviorGroupDetailsPanel
              group={this.getBehaviorGroup()}
              onBehaviorGroupNameChange={this.onBehaviorGroupNameChange}
              onBehaviorGroupDescriptionChange={this.onBehaviorGroupDescriptionChange}
              onBehaviorGroupIconChange={this.onBehaviorGroupIconChange}
              onDone={this.props.onClearActivePanel}
              visible={this.props.activePanelName === 'requestBehaviorGroupDetails'}
            />
          </Collapsible>

          <Collapsible ref={(el) => this.props.onRenderPanel("configureApi", el)}
            revealWhen={this.props.activePanelName === "configureApi"}
            onChange={this.layoutDidUpdate}
            animationDisabled={this.state.animationDisabled}
          >
            <APIConfigPanel
              openWhen={this.getActiveDropdown() === 'apiConfigAdderDropdown'}
              toggle={this.toggleApiAdderDropdown}
              getActiveDropdown={this.getActiveDropdown}
              requiredConfig={this.getSelectedApiConfig()}
              getApiLogoUrlForConfig={this.getApiLogoUrlForConfig}
              getApiNameForConfig={this.getApiNameForConfig}
              getApiConfigName={this.getApiConfigName}
              allConfigs={this.getApiConfigsForSelected()}
              onAddConfig={this.onAddConfigForSelected()}
              onAddNewConfig={this.onAddNewConfigForSelected()}
              onRemoveConfig={this.onRemoveConfigForSelected()}
              onUpdateConfig={this.onUpdateConfigForSelected()}
              onDoneClick={this.props.onClearActivePanel}
              addNewAWSConfig={this.addNewAWSConfig}
              addNewOAuth2Application={this.addNewOAuth2Application}
              animationDisabled={this.state.animationDisabled}
            >
            </APIConfigPanel>
          </Collapsible>

          <Collapsible ref={(el) => this.props.onRenderPanel("confirmUndo", el)} revealWhen={this.props.activePanelName === 'confirmUndo'} onChange={this.layoutDidUpdate}>
            <ConfirmActionPanel confirmText="Undo changes" onConfirmClick={this.undoChanges} onCancelClick={this.toggleConfirmUndo}>
              <p>This will undo any changes you’ve made since last saving. Are you sure you want to do this?</p>
            </ConfirmActionPanel>
          </Collapsible>

          <Collapsible ref={(el) => this.props.onRenderPanel("confirmDeleteEditable", el)} revealWhen={this.props.activePanelName === 'confirmRevert'} onChange={this.layoutDidUpdate}>
            <ConfirmActionPanel confirmText="Switch versions" onConfirmClick={this.doRevert} onCancelClick={this.toggleConfirmRevert}>
              {this.confirmRevertText()}
            </ConfirmActionPanel>
          </Collapsible>

          <Collapsible ref={(el) => this.props.onRenderPanel("confirmDeleteEditable", el)} revealWhen={this.props.activePanelName === 'confirmDeleteEditable'} onChange={this.layoutDidUpdate}>
            <ConfirmActionPanel confirmText="Delete" onConfirmClick={this.deleteEditable} onCancelClick={this.props.onClearActivePanel}>
              <p>{this.confirmDeleteText()}</p>
            </ConfirmActionPanel>
          </Collapsible>

          <Collapsible ref={(el) => this.props.onRenderPanel("confirmDeleteBehaviorGroup", el)} revealWhen={this.props.activePanelName === 'confirmDeleteBehaviorGroup'} onChange={this.layoutDidUpdate}>
            <ConfirmActionPanel confirmText="Delete" onConfirmClick={this.deleteBehaviorGroup} onCancelClick={this.props.onClearActivePanel}>
              <p>Are you sure you want to delete this skill and all of its actions and data types?</p>
            </ConfirmActionPanel>
          </Collapsible>

          <Collapsible revealWhen={this.props.activePanelName === 'helpForTriggerParameters'} onChange={this.layoutDidUpdate}>
            <TriggerHelp onCollapseClick={this.props.onClearActivePanel} />
          </Collapsible>

          <Collapsible revealWhen={this.props.activePanelName === 'helpForUserInput'} onChange={this.layoutDidUpdate}>
            <UserInputHelp onCollapseClick={this.props.onClearActivePanel} />
          </Collapsible>

          <Collapsible revealWhen={this.props.activePanelName === 'helpForBehaviorCode'} onChange={this.layoutDidUpdate}>
            <BehaviorCodeHelp
              envVariableNames={this.getEnvVariableNames()}
              apiAccessTokens={this.getApiApplications()}
              onAddNewEnvVariable={this.onAddNewEnvVariable}
              onCollapseClick={this.props.onClearActivePanel}
              isDataType={this.isDataTypeBehavior()}
            />
          </Collapsible>

          <Collapsible revealWhen={this.props.activePanelName === 'helpForDevModeChannels'} onChange={this.layoutDidUpdate}>
            <DevModeChannelsHelp
              onCollapseClick={this.props.onClearActivePanel}
              slackTeamId={this.props.slackTeamId}
              botName={this.props.botName}
            />
          </Collapsible>

          <Collapsible revealWhen={this.props.activePanelName === 'helpForDataTypeSource'} onChange={this.layoutDidUpdate}>
            <DataTypeSourceHelp onCollapseClick={this.props.onClearActivePanel} />
          </Collapsible>

          <Collapsible revealWhen={this.props.activePanelName === 'helpForDataTypePrompt'} onChange={this.layoutDidUpdate}>
            <DataTypePromptHelp usesSearch={this.hasInputNamed('searchQuery')} onCollapseClick={this.props.onClearActivePanel} />
          </Collapsible>

          <Collapsible revealWhen={this.props.activePanelName === 'helpForLibraryCode'} onChange={this.layoutDidUpdate}>
            <LibraryCodeHelp onCollapseClick={this.props.onClearActivePanel} libraryName={this.getEditableName()} />
          </Collapsible>

          <Collapsible revealWhen={this.props.activePanelName === 'helpForResponseTemplate'} onChange={this.layoutDidUpdate}>
            <ResponseTemplateHelp
              firstParamName={this.getFirstBehaviorInputName()}
              template={this.getBehaviorTemplate()}
              onCollapseClick={this.props.onClearActivePanel}
            />
          </Collapsible>

          <Collapsible ref={(el) => this.props.onRenderPanel("envVariableSetter", el)} revealWhen={this.props.activePanelName === 'envVariableSetter'} onChange={this.layoutDidUpdate}>
            <div className="box-action phn">
              <div className="container">
                <div className="columns">
                  <div className="column column-page-sidebar" />
                  <div className="column column-page-main">
                    <EnvVariableSetter
                      ref={(el) => this.envVariableSetterPanel = el}
                      vars={this.getEnvVariables()}
                      onCancelClick={this.props.onClearActivePanel}
                      onSave={this.updateEnvVariables}
                    />
                  </div>
                </div>
              </div>
            </div>
          </Collapsible>

          <Collapsible ref={(el) => this.props.onRenderPanel("envVariableAdder", el)} revealWhen={this.props.activePanelName === 'envVariableAdder'} onChange={this.layoutDidUpdate}>
            <div className="box-action phn">
              <div className="container">
                <div className="columns">
                  <div className="column column-page-sidebar" />
                  <div className="column column-page-main">
                    <EnvVariableAdder
                      ref={(el) => this.envVariableAdderPanel = el}
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
              ref={(el) => this.props.onRenderPanel("behaviorTester", el)}
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
              ref={(el) => this.props.onRenderPanel("dataTypeTester", el)}
              behaviorId={this.getSelectedId()}
              isSearch={this.isSearchDataTypeBehavior()}
              csrfToken={this.props.csrfToken}
              onDone={this.props.onClearActivePanel}
              appsRequiringAuth={this.getOAuth2ApplicationsRequiringAuth()}
            />
          </Collapsible>

          {this.getOtherSavedInputsInGroup().length > 0 ? (
            <Collapsible ref={(el) => this.props.onRenderPanel("sharedAnswerInputSelector", el)} revealWhen={this.props.activePanelName === 'sharedAnswerInputSelector'} onChange={this.layoutDidUpdate}>
              <SharedAnswerInputSelector
                onToggle={this.toggleSharedAnswerInputSelector}
                onSelect={this.addInput}
                inputs={this.getOtherSavedInputsInGroup()}
              />
            </Collapsible>
          ) : null}

          <Collapsible ref={(el) => this.props.onRenderPanel("savedAnswerEditor", el)} revealWhen={this.props.activePanelName === 'savedAnswerEditor'} onChange={this.layoutDidUpdate}>
            <SavedAnswerEditor
              onToggle={this.toggleSavedAnswerEditor}
              savedAnswers={this.props.savedAnswers}
              selectedInput={this.getInputWithSavedAnswers()}
              onForgetSavedAnswerForUser={this.forgetSavedAnswerForUser}
              onForgetSavedAnswersForTeam={this.forgetSavedAnswersForTeam}
            />
          </Collapsible>

          <Collapsible ref={(el) => this.props.onRenderPanel("saving", el)} revealWhen={this.isSaving()} onChange={this.layoutDidUpdate}>
            <div className="box-action">
              <div className="container phn">
                <p className="align-c">
                  <b className="pulse">Saving changes…</b>
                </p>
              </div>
            </div>
          </Collapsible>

          <Collapsible revealWhen={!this.props.activePanelIsModal && this.props.activePanelName !== 'versionBrowser'} onChange={this.layoutDidUpdate} animationDisabled={this.animationIsDisabled()}>
            <Notifications notifications={this.getNotifications()} />
            <div className="container container-wide ptm border-top">
              <div>
                <div>
                  <DynamicLabelButton
                    onClick={this.onSaveClick}
                    labels={[{
                      text: 'Save',
                      displayWhen: !this.isExistingGroup()
                    }, {
                      text: 'Save changes',
                      mobileText: 'Save',
                      displayWhen: this.isExistingGroup() && !this.isJustSaved()
                    }, {
                      text: 'Saved',
                      displayWhen: this.isJustSaved()
                    }]}
                    className="button-primary mrs mbm"
                    disabledWhen={!this.isModified() || this.isSaving()}
                  />
                  {this.isExistingGroup() ? (
                    <Button className="mrs mbm" disabled={!this.isModified() || this.isSaving()} onClick={this.toggleConfirmUndo}>
                      <span className="mobile-display-none">Undo changes</span>
                      <span className="mobile-display-only">Undo</span>
                    </Button>
                  ) : null}
                  {this.isTestable() ? (
                    <DynamicLabelButton
                      labels={[{
                        text: 'Test…',
                        displayWhen: !this.isModified()
                      }, {
                        text: 'Save and test…',
                        displayWhen: this.isModified()
                      }]}
                      disabledWhen={!this.isExisting() && !this.isModified()}
                      className={`mbm ${this.isExistingGroup() ? "mrs" : "mrl"}`} onClick={this.checkIfModifiedAndTest}
                    />) : null}
                  {this.isExistingGroup() ? (
                    <Button
                      className="mrl mbm"
                      onClick={this.showVersions}>
                      Review changes…
                    </Button>
                  ) : null}
                  <div className="display-inline-block align-button mbm">
                    {this.renderFooterStatus()}
                  </div>
                </div>
              </div>
            </div>
          </Collapsible>
      </div>
    ), footerClassName);
  },

  renderFooterStatus: function() {
    if (this.state.error === 'not_saved') {
      return (
        <span className="fade-in type-pink type-bold type-italic">
          <span style={{ height: 24 }} className="display-inline-block mrs align-b"><SVGWarning /></span>
          <span>Error saving changes — please try again</span>
        </span>
      );
    } else if (this.state.error === 'not_deployed') {
      return (
        <span className="fade-in type-pink type-bold type-italic">
          <span style={{ height: 24 }} className="display-inline-block mrs align-b"><SVGWarning /></span>
          <span>Error deploying — please try again</span>
        </span>
      );
    } else if (this.state.error) {
      return (
        <span className="fade-in type-pink type-bold type-italic">
          <span style={{ height: 24 }} className="display-inline-block mrs align-b"><SVGWarning /></span>
          <span>{this.state.error}</span>
        </span>
      );
    } else {
      return "";
    }
  },

  renderHiddenForms: function() {
    return (
      <div>
        <form ref={(el) => this.deleteBehaviorGroupForm = el} action={jsRoutes.controllers.ApplicationController.deleteBehaviorGroups().url} method="POST">
          <CsrfTokenHiddenInput value={this.props.csrfToken} />
          <input type="hidden" name="behaviorGroupIds[0]" value={this.getBehaviorGroup().id || ""} />
        </form>
      </div>
    );
  },

  behaviorSwitcherIsVisible: function() {
    return this.state.behaviorSwitcherVisible;
  },

  mobileBehaviorSwitcherIsVisible: function() {
    return this.hasMobileLayout() && this.behaviorSwitcherIsVisible();
  },

  renderSwitcherToggle: function() {
    if (this.windowIsMobile()) {
      return (
        <div className="bg-white container container-wide type-weak border-bottom display-ellipsis display-limit-width">
          <Button className="button-tab button-tab-subtle" onClick={this.toggleBehaviorSwitcher}>
            <span className="display-inline-block align-t mrm" style={{ height: "24px" }}>
              <SVGHamburger />
            </span>
            <h4 className="type-black display-inline-block align-m man">Skill components</h4>
          </Button>
        </div>
      );
    }
  },

  getEditorScrollPosition: function() {
    const selected = this.getSelected();
    return selected ? selected.editorScrollPosition : 0;
  },

  onSelect: function(optionalGroupId, id, optionalCallback) {
    var newState = {
      animationDisabled: true,
      selectedId: id
    };
    if (this.windowIsMobile()) {
      newState.behaviorSwitcherVisible = false;
    }
    this.setState(newState, () => {
      if (optionalGroupId) {
        BrowserUtils.replaceURL(jsRoutes.controllers.BehaviorEditorController.edit(optionalGroupId, id).url);
      }
      var newScrollPosition = this.getEditorScrollPosition();
      window.scrollTo(window.scrollX, typeof(newScrollPosition) === 'number' ? newScrollPosition : 0);
      this.setState({
        animationDisabled: false
      });
      if (optionalCallback) {
        optionalCallback();
      }
    });
  },

  animationIsDisabled: function() {
    return this.state.animationDisabled;
  },

  addNewBehavior: function(isDataType, behaviorIdToClone, optionalDefaultProps) {
    const group = this.getBehaviorGroup();
    const newName = optionalDefaultProps ? optionalDefaultProps.name : null;
    const url = jsRoutes.controllers.BehaviorEditorController.newUnsavedBehavior(isDataType, group.teamId, behaviorIdToClone, newName).url;
    fetch(url, { credentials: 'same-origin' })
      .then((response) => response.json())
      .then((json) => {
        const newVersion = BehaviorVersion.fromJson(Object.assign({}, json, { groupId: group.id })).clone(optionalDefaultProps || {});
        const groupWithNewBehavior = group.withNewBehaviorVersion(newVersion);
        this.updateGroupStateWith(groupWithNewBehavior, () => {
          this.onSelect(groupWithNewBehavior.id, newVersion.behaviorId);
        });
      });
  },

  addNewAction: function() {
    const nextActionName = SequentialName.nextFor(this.getActionBehaviors(), (ea) => ea.name, "action");
    this.addNewBehavior(false, null, BehaviorVersion.defaultActionProps(nextActionName));
  },

  addNewDataType: function() {
    const nextDataTypeName = SequentialName.nextFor(this.getDataTypeBehaviors(), (ea) => ea.name, "DataType");
    this.addNewBehavior(true, null, { name: nextDataTypeName });
  },

  addNewLibraryImpl: function(libraryIdToClone, optionalProps) {
    const group = this.getBehaviorGroup();
    const url = jsRoutes.controllers.BehaviorEditorController.newUnsavedLibrary(group.teamId, libraryIdToClone).url;
    fetch(url, { credentials: 'same-origin' })
      .then((response) => response.json())
      .then((json) => {
        const newVersion = LibraryVersion.fromProps(Object.assign({}, json, { groupId: group.id })).clone(optionalProps || {});
        const groupWithNewLibrary = group.withNewLibraryVersion(newVersion);
        this.updateGroupStateWith(groupWithNewLibrary, () => {
          this.onSelect(groupWithNewLibrary.id, newVersion.libraryId);
        });
      });
  },

  addNewLibrary: function() {
    const nextLibraryName = SequentialName.nextFor(this.getLibraries(), (ea) => ea.name, "library");
    this.addNewLibraryImpl(null, {
      name: nextLibraryName,
      functionBody: LibraryVersion.defaultLibraryCode()
    });
  },

  cloneLibrary: function(libraryIdToClone) {
    this.addNewLibraryImpl(libraryIdToClone);
  },

  selectRequiredApiConfig: function(required, callback) {
    this.setState({
      selectedApiConfigId: required.id
    }, callback);
  },

  toggleConfigureApiPanel: function() {
    this.props.onToggleActivePanel("configureApi", true);
  },

  onApiConfigClick: function(required) {
    this.selectRequiredApiConfig(required, this.toggleConfigureApiPanel);
  },

  onAddApiConfigClick: function() {
    this.setState({
      selectedApiConfigId: null
    }, this.toggleConfigureApiPanel);
  },

  renderBehaviorSwitcher: function() {
    return (
      <div className={
          "column column-page-sidebar flex-column flex-column-left bg-white " +
          "border-right-thick border-light prn position-relative mobile-position-fixed-top-full mobile-position-z-front "
        }
      >
        <Collapsible revealWhen={this.behaviorSwitcherIsVisible()} animationDisabled={!this.hasMobileLayout()}>
          <Sticky
            ref={(el) => this.leftPanel = el}
            onGetCoordinates={this.getLeftPanelCoordinates}
            innerClassName="position-z-above"
            disabledWhen={this.hasMobileLayout()}
          >
            {this.windowIsMobile() ? (
              <div className="position-absolute position-top-right mtm mobile-mts mobile-mrs">
                <CollapseButton onClick={this.toggleBehaviorSwitcher} direction={"up"} />
              </div>
            ) : null}
            <BehaviorSwitcher
              actionBehaviors={this.getActionBehaviors()}
              dataTypeBehaviors={this.getDataTypeBehaviors()}
              libraries={this.getLibraries()}
              nodeModuleVersions={this.getNodeModuleVersions()}
              selectedId={this.getSelectedId()}
              groupId={this.getBehaviorGroup().id}
              onSelect={this.onSelect}
              addNewAction={this.addNewAction}
              addNewDataType={this.addNewDataType}
              addNewLibrary={this.addNewLibrary}
              isModified={this.editableIsModified}
              onUpdateNodeModules={this.updateNodeModules}
              requiredAWSConfigs={this.getRequiredAWSConfigs()}
              requiredOAuth2Applications={this.getRequiredOAuth2ApiConfigs()}
              requiredSimpleTokenApis={this.getRequiredSimpleTokenApis()}
              onApiConfigClick={this.onApiConfigClick}
              onAddApiConfigClick={this.onAddApiConfigClick}
              getApiConfigName={this.getApiConfigName}
            />
          </Sticky>
        </Collapsible>
      </div>
    );
  },

  renderNameAndManagementActions: function() {
    return (
      <div className="container container-wide bg-white">
        <div className="columns columns-elastic mobile-columns-float">
          <div className="column column-shrink">
            <FormInput
              className="form-input-borderless form-input-l type-l type-semibold width-15 mobile-width-full"
              ref={(el) => this.editableNameInput = el}
              value={this.getEditableName()}
              placeholder={this.getSelected().namePlaceholderText()}
              onChange={this.updateName}
            />
          </div>
          <div className="column column-expand align-r align-m mobile-align-l mobile-mtl">
            {this.isExisting() ? (
              <div>
                <div className="mobile-display-inline-block mobile-mrs align-t">
                  <Button
                    className="button-s mbs"
                    onClick={this.cloneEditable}>
                    {this.getSelected().cloneActionText()}
                  </Button>
                </div>
                <div className="mobile-display-inline-block align-t">
                  <Button
                    className="button-s"
                    onClick={this.confirmDeleteEditable}>
                    {this.getSelected().deleteActionText()}
                  </Button>
                </div>
              </div>
            ) : (
              <div>
                <Button
                  className="button-s"
                  onClick={this.deleteEditable}
                >{this.getSelected().cancelNewText()}</Button>
              </div>
            )}
          </div>
        </div>
      </div>
    );
  },

  renderNormalBehavior: function() {
    return (

      <div>
                <div className="columns container container-wide bg-white">
                  <div className="column column-full mobile-column-full">
                    <FormInput
                      className="form-input-borderless form-input-m mbneg1"
                      placeholder="Action description (optional)"
                      onChange={this.updateDescription}
                      value={this.getEditableDescription()}
                    />
                  </div>
                </div>

                <hr className="mtn mbn rule-subtle" />

                <TriggerConfiguration
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
                  onInputChange={this.updateBehaviorInputAtIndexWith}
                  onInputMove={this.moveBehaviorInputAtIndex}
                  onInputDelete={this.deleteInputAtIndex}
                  onInputAdd={this.addNewInput}
                  onInputNameFocus={this.onInputNameFocus}
                  onInputNameBlur={this.onInputNameBlur}
                  onConfigureType={this.onConfigureType}
                  userInputs={this.getInputs()}
                  paramTypes={this.getParamTypes()}
                  triggers={this.getBehaviorTriggers()}
                  hasSharedAnswers={this.getOtherSavedInputsInGroup().length > 0}
                  otherBehaviorsInGroup={this.otherBehaviorsInGroup()}
                  onToggleSharedAnswer={this.toggleSharedAnswerInputSelector}
                  savedAnswers={this.props.savedAnswers}
                  onToggleSavedAnswer={this.toggleSavedAnswerEditor}
                  onToggleInputHelp={this.toggleUserInputHelp}
                  helpInputVisible={this.props.activePanelName === 'helpForUserInput'}
                />

                <hr className="man rule-subtle" />

                <div>
                  {this.renderCodeEditor({
                    sectionNumber: "3",
                    codeHelpPanelName: 'helpForBehaviorCode'
                  })}

                  <hr className="man rule-subtle" />
                </div>

                <ResponseTemplateConfiguration
                  template={this.getBehaviorTemplate()}
                  onChangeTemplate={this.updateTemplate}
                  isFinishedBehavior={this.isFinishedBehavior()}
                  behaviorUsesCode={this.getFunctionBody().length > 0}
                  shouldForcePrivateResponse={this.shouldForcePrivateResponse()}
                  onChangeForcePrivateResponse={this.updateForcePrivateResponse}
                  onCursorChange={this.ensureCursorVisible}
                  onToggleHelp={this.toggleResponseTemplateHelp}
                  helpVisible={this.props.activePanelName === 'helpForResponseTemplate'}
                  sectionNumber={"4"}
                />

      </div>
    );
  },

  renderDataTypeBehavior: function() {
    return (
      <div>
        <div className="bg-white pbl" />
        <hr className="mtn mbn rule-subtle" />

        <DataTypeEditor
          ref={(el) => this.dataTypeEditor = el}
          group={this.getBehaviorGroup()}
          behaviorVersion={this.getSelectedBehavior()}
          paramTypes={this.getParamTypesForDataTypes()}
          inputs={this.getInputs()}
          onChangeConfig={this.setConfigProps}
          onChangeCode={this.updateCode}
          onAddNewInput={this.addNewInput}
          onDeleteInputs={this.deleteAllInputs}
          onConfigureType={this.onConfigureType}
          isModified={this.editableIsModified}

          activePanelName={this.props.activePanelName}
          activeDropdownName={this.getActiveDropdown()}
          onToggleActiveDropdown={this.toggleActiveDropdown}
          onToggleActivePanel={this.toggleActivePanel}
          animationIsDisabled={this.animationIsDisabled()}

          behaviorConfig={this.getBehaviorConfig()}

          systemParams={this.getSystemParams()}
          requiredAWSConfigs={this.getRequiredAWSConfigs()}
          apiApplications={this.getApiApplications()}

          onCursorChange={this.ensureCursorVisible}
          useLineWrapping={this.state.codeEditorUseLineWrapping}
          onToggleCodeEditorLineWrapping={this.toggleCodeEditorLineWrapping}

          envVariableNames={this.getEnvVariableNames()}
        />
      </div>
    );
  },

  renderForSelected: function(selected) {
    if (selected.isDataType()) {
      return this.renderDataTypeBehavior();
    } else if (selected.isBehaviorVersion()) {
      return this.renderNormalBehavior();
    } else if (selected.isLibraryVersion()) {
      return this.renderLibrary();
    }
  },

  renderLibrary: function() {
    return (
      <div className="pbxxxl">

        <div className="columns container container-wide bg-white">
          <div className="column column-full mobile-column-full">
            <FormInput
              className="form-input-borderless form-input-m mbneg1"
              placeholder="Library description (optional)"
              onChange={this.updateDescription}
              value={this.getEditableDescription()}
            />
          </div>
        </div>

        <hr className="man rule-subtle" />

        {this.renderCodeEditor({
          systemParams: [],
          sectionNumber: "1",
          codeHelpPanelName: 'helpForLibraryCode',
          functionExecutesImmediately: true
        })}
      </div>
    );
  },

  renderEditor: function() {
    const selected = this.getSelected();
    if (selected) {
      return (
        <div>
          <div className="container container-wide ptl bg-white">
            <h5 className="type-blue-faded mvn">{selected.getEditorTitle()}</h5>
          </div>

          {this.renderNameAndManagementActions()}
          {this.renderForSelected(selected)}
        </div>
      );
    } else {
      return (
        <BehaviorGroupEditor
          group={this.getBehaviorGroup()}
          isModified={this.isModified()}
          onBehaviorGroupNameChange={this.onBehaviorGroupNameChange}
          onBehaviorGroupDescriptionChange={this.onBehaviorGroupDescriptionChange}
          onBehaviorGroupIconChange={this.onBehaviorGroupIconChange}
          onDeleteClick={this.confirmDeleteBehaviorGroup}
        />
      );
    }
  },

  renderEditorPage: function() {
    return (
      <div className="flex-row-cascade">
        <form className="flex-row-cascade" action={this.getFormAction()} method="POST" ref={(el) => this.behaviorForm = el}>
          <div className="flex-row-cascade">
            <div className="flex-column flex-column-left flex-rows">
              <div className={`columns flex-columns flex-row-expand mobile-flex-no-columns ${
                (this.props.activePanelName === 'versionBrowser' || this.state.versionBrowserOpen) ? "position-frozen" : ""
               }`}>
                {this.renderBehaviorSwitcher()}
                <div
                  className="column column-page-main column-page-main-wide flex-column flex-column-main"
                  style={{ paddingBottom: `${this.props.footerHeight}px `}}
                >
                  {this.renderSwitcherToggle()}

                  {this.renderEditor()}
                </div>
              </div>
            </div>
          </div>

          {this.renderFooter()}

          {this.renderHiddenFormValues()}
        </form>

        {this.renderHiddenForms()}

      </div>
    );
  },

  renderVersionBrowser: function() {
    return (
      <VersionBrowser
        csrfToken={this.props.csrfToken}
        currentGroup={this.getBehaviorGroup()}
        currentGroupIsModified={this.isModified()}
        currentUserId={this.props.userId}
        currentSelectedId={this.getSelectedId()}
        versions={this.getVersions()}
        onRestoreVersionClick={this.confirmRevert}
        onUndoChanges={this.toggleConfirmUndo}
        onClearActivePanel={this.props.onClearActivePanel}
        editableIsModified={this.editableIsModified}
        isLinkedToGithub={this.props.isLinkedToGithub}
        linkedGithubRepo={this.props.linkedGithubRepo}
        onLinkGithubRepo={this.props.onLinkGithubRepo}
        onChangedGithubRepo={this.toggleChangeGithubRepo}
        onUpdateFromGithub={this.props.onUpdateFromGithub}
        onSaveChanges={this.onSaveClick}
        isModifyingGithubRepo={this.state.isModifyingGithubRepo}
      />
    );
  },

  renderNavItems: function() {
    const versionBrowserOpen = this.props.activePanelName === 'versionBrowser';
    const indexUrl = jsRoutes.controllers.ApplicationController.index(this.props.group.teamId).url;
    const items = [{
      title: "Skills",
      url: indexUrl
    }, {
      title: this.getBehaviorGroup().getName(),
      callback: versionBrowserOpen ? this.props.onClearActivePanel : null
    }];
    if (versionBrowserOpen) {
      items.push({
        title: "Review changes"
      });
    }
    this.props.onRenderNavItems(items);
  },

  renderDeployStatus: function() {
    return (
      <DeploymentStatus
        group={this.getBehaviorGroup()}
        isModified={this.isModified()}
        lastSaveTimestamp={this.props.group.createdAt}
        lastDeployTimestamp={this.props.lastDeployTimestamp}
        currentUserId={this.props.userId}
        onDevModeChannelsClick={this.toggleDevModeChannelsHelp}
        onDeployClick={this.deploy}
      />
    );
  },

  renderGithubRepoActions: function() {
    return (
      <GithubRepoActions
        linkedGithubRepo={this.props.linkedGithubRepo}
        isLinkedToGithub={this.props.isLinkedToGithub}
        currentGroupIsModified={this.isModified()}
        currentGroup={this.getBehaviorGroup()}
        currentSelectedId={this.getSelectedId()}
        onChangeGithubRepo={this.toggleChangeGithubRepo}
      />
    );
  },

  renderNavActions: function() {
    if (this.state.versionBrowserOpen) {
      this.props.onRenderNavActions(this.renderGithubRepoActions());
    } else {
      this.props.onRenderNavActions(this.renderDeployStatus());
    }
  },

  render: function() {
    const versionBrowserShouldOpen = this.props.activePanelName === 'versionBrowser';
    return (
      <div className="position-relative flex-row-cascade">
        <Collapsible className="flex-row-cascade" revealWhen={versionBrowserShouldOpen} onChange={this.updateVersionBrowserOpenState}>
          {this.renderVersionBrowser()}
        </Collapsible>

        <div className={versionBrowserShouldOpen ? "" : "flex-row-cascade"}>
          {this.renderEditorPage()}
        </div>
      </div>
    );
  }
});

export default BehaviorEditor;
