import * as React from 'react';
import APIConfigPanel from './api_config_panel';
import {AWSConfigRef} from '../models/aws';
import BehaviorGroup, {BehaviorGroupJson} from '../models/behavior_group';
import BehaviorGroupVersionMetaData from '../models/behavior_group_version_meta_data';
import BehaviorGroupDetailsPanel from './behavior_group_details_panel';
import BehaviorGroupEditor from './behavior_group_editor';
import BehaviorVersion, {BehaviorVersionInterface, BehaviorVersionJson} from '../models/behavior_version';
import BehaviorSwitcher from './behavior_switcher';
import BehaviorTester from './behavior_tester';
import DataTypeTester from './data_type_tester';
import BehaviorTestResult, {BehaviorTestResultsJson} from '../models/behavior_test_result';
import BehaviorCodeHelp from './behavior_code_help';
import Button from '../form/button';
import CodeConfiguration from './code_configuration';
import ConfirmActionPanel from '../panels/confirm_action';
import CollapseButton from '../shared_ui/collapse_button';
import {DataRequest, ResponseError} from '../lib/data_request';
import DataTypeEditor from './data_type_editor';
import DataTypeSourceHelp from './data_type_source_help';
import DefaultStorageAdder from './default_storage_adder';
import DefaultStorageBrowser from './default_storage_browser';
import DevModeChannelsHelp from './dev_mode_channels_help';
import DropdownContainer from '../shared_ui/dropdown_container';
import DynamicLabelButton from '../form/dynamic_label_button';
import EnvVariableSetter from '../settings/environment_variables/setter';
import Input from '../models/input';
import Formatter, {Timestamp} from '../lib/formatter';
import ID from '../lib/id';
import NodeModuleVersion, {NodeModuleVersionJson} from '../models/node_module_version';
import FormInput from '../form/input';
import LibraryCodeHelp from './library_code_help';
import LibraryVersion, {LibraryVersionInterface, LibraryVersionJson} from '../models/library_version';
import LinkedGithubRepo from '../models/linked_github_repo';
import ModalScrim from '../shared_ui/modal_scrim';
import Notifications from '../notifications/notifications';
import {OAuthApiJson, OAuthApplicationRef} from '../models/oauth';
import {NavItemContent, PageRequiredProps} from '../shared_ui/page';
import ParamType from '../models/param_type';
import {RequiredAWSConfig} from '../models/aws';
import {RequiredOAuthApplication} from '../models/oauth';
import ResponseTemplate from '../models/response_template';
import ResponseTemplateConfiguration from './response_template_configuration';
import ResponseTemplateHelp from './response_template_help';
import SavedAnswerEditor from './saved_answer_editor';
import SequentialName from '../lib/sequential_name';
import SharedAnswerInputSelector from './shared_answer_input_selector';
import {RequiredSimpleTokenApi, SimpleTokenApiRef} from '../models/simple_token';
import Sticky, {Coords} from '../shared_ui/sticky';
import SVGHamburger from '../svg/hamburger';
import TriggerConfiguration from './trigger_configuration';
import TriggerHelp from './trigger_help';
import UserInputConfiguration, {SavedAnswer} from './user_input_configuration';
import UserInputHelp from './user_input_help';
import VersionBrowser from './versions/version_browser';
import SVGWarning from '../svg/warning';
import Collapsible from '../shared_ui/collapsible';
import CsrfTokenHiddenInput from '../shared_ui/csrf_token_hidden_input';
import BrowserUtils from '../lib/browser_utils';
import Event from '../lib/event';
import ImmutableObjectUtils from '../lib/immutable_object_utils';
import * as debounce from 'javascript-debounce';
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
import OAuthConfigWithoutApplicationNotificationData from "../models/notifications/oauth_config_without_application_notification_data";
import ServerDataWarningNotificationData from "../models/notifications/server_data_warning_notification_data";
import SkillDetailsWarningNotificationData from "../models/notifications/skill_details_warning_notification_data";
import TestResultsNotificationData from "../models/notifications/test_result_notification_data";
import UnknownParamInTemplateNotificationData from "../models/notifications/unknown_param_in_template_notification_data";
import TestOutput from "./test_output";
import autobind from "../lib/autobind";
import {EnvironmentVariableData, EnvironmentVariablesData} from "../settings/environment_variables/loader";
import NotificationData from "../models/notifications/notification_data";
import {GithubFetchError} from "../models/github/github_fetch_error";
import {UpdateFromGithubSuccessData} from "./loader";
import {BehaviorGroupDeploymentJson} from "../models/behavior_group_deployment";
import BehaviorResponseType from "../models/behavior_response_type";
import ApiConfigRef, {ApiJson} from "../models/api_config_ref";
import RequiredApiConfig from "../models/required_api_config";
import Editable, {EditableInterface} from "../models/editable";
import Trigger from "../models/trigger";
import BehaviorConfig, {BehaviorConfigInterface} from "../models/behavior_config";
import {EditorCursorPosition} from "./code_editor";

export interface BehaviorEditorProps {
  group: BehaviorGroup
  selectedId: Option<string>
  csrfToken: string
  builtinParamTypes: Array<ParamType>
  envVariables: Array<EnvironmentVariableData>
  awsConfigs: Array<AWSConfigRef>
  oauthApplications: Array<OAuthApplicationRef>
  oauthApis: Array<OAuthApiJson>
  simpleTokenApis: Array<SimpleTokenApiRef>
  linkedOAuthApplicationIds: Array<string>
  linkedGithubRepo?: Option<LinkedGithubRepo>
  savedAnswers: Array<SavedAnswer>
  onSave: (newProps: { group: BehaviorGroup, onLoad?: Option<() => void> }) => void
  onLinkGithubRepo: (owner: string, repo: string, branch: Option<string>, callback?: () => void) => void
  onUpdateFromGithub: (owner: string, repo: string, branch: string, callback: (json: UpdateFromGithubSuccessData) => void, onError: (branch: string, error?: Option<GithubFetchError>) => void) => void
  onForgetSavedAnswerForInput: (inputId: string, numAnswersDeleted: number) => void
  onLoad: Option<() => void>
  userId: string
  isAdmin: boolean
  isLinkedToGithub: boolean
  showVersions: Option<boolean>
  onDeploy: (deploymentProps: BehaviorGroupDeploymentJson, callback?: () => void) => void
  lastDeployTimestamp: Option<Timestamp>
  slackTeamId: Option<string>
  botName: string
  possibleResponseTypes: Array<BehaviorResponseType>
}

type Props = BehaviorEditorProps & PageRequiredProps;

enum VersionsLoadStatus {
  Loading = 'loading',
  Loaded = 'loaded',
  Error = 'error',
  None = ''
}

interface ActiveDropdown {
  name: string
}

interface State {
  group: BehaviorGroup
  selectedId: Option<string>
  activeDropdown: Option<ActiveDropdown>
  codeEditorUseLineWrapping: boolean
  envVariables: Array<EnvironmentVariableData>
  notifications: Array<NotificationData>
  versions: Array<BehaviorGroup>
  versionsLoadStatus: VersionsLoadStatus
  requiredAWSConfig: Option<RequiredAWSConfig>
  shouldRedirectToAddNewAWSConfig: boolean
  requiredOAuthApiConfig: Option<RequiredOAuthApplication>
  shouldRedirectToAddNewOAuthApp: boolean
  paramNameToSync: Option<string>
  error: Option<string>
  selectedSavedAnswerInputId: Option<string>
  behaviorSwitcherVisible: boolean
  hasMobileLayout: boolean
  animationDisabled: boolean
  nodeModuleVersions: Array<NodeModuleVersion>
  selectedApiConfigId: Option<string>
  newerVersionOnServer: Option<BehaviorGroupVersionMetaData>
  errorReachingServer: Option<Error>
  versionBrowserOpen: boolean
  revertToVersion: Option<BehaviorGroup>
  revertToVersionTitle: Option<string>
  isModifyingGithubRepo: boolean
  updatingNodeModules: boolean
  testResults: Array<BehaviorTestResult>
  runningTests: boolean
}

interface CodeConfigProps {
  sectionNumber: string
  codeHelpPanelName: string
  systemParams?: Array<string>
  isMemoizationEnabled: boolean
  functionExecutesImmediately?: boolean
}

class BehaviorEditor extends React.Component<Props, State> {
  resetNotificationsEventually: () => void;
  editableNameInput: Option<FormInput>;
  dataTypeEditor: Option<DataTypeEditor>;
  deleteBehaviorGroupForm: Option<HTMLFormElement>;
  leftPanel: Option<Sticky>;
  behaviorForm: Option<HTMLFormElement>;
  envVariableSetterPanel: Option<EnvVariableSetter>;
  checkForUpdateTimer: number | undefined;

  constructor(props: Props) {
    super(props);
    autobind(this);

    this.resetNotificationsEventually = debounce(() => {
      this.resetNotificationsImmediately();
    }, 500);

    this.state = {
      group: this.props.group,
      selectedId: this.props.selectedId,
      activeDropdown: null,
      codeEditorUseLineWrapping: false,
      envVariables: this.getInitialEnvVariables(),
      notifications: this.buildNotifications(),
      versions: [],
      versionsLoadStatus: VersionsLoadStatus.None,
      requiredAWSConfig: null,
      shouldRedirectToAddNewAWSConfig: false,
      requiredOAuthApiConfig: null,
      shouldRedirectToAddNewOAuthApp: false,
      paramNameToSync: null,
      error: null,
      selectedSavedAnswerInputId: null,
      behaviorSwitcherVisible: !this.windowIsMobile(),
      hasMobileLayout: this.windowIsMobile(),
      animationDisabled: false,
      nodeModuleVersions: [],
      selectedApiConfigId: null,
      newerVersionOnServer: null,
      errorReachingServer: null,
      versionBrowserOpen: false,
      revertToVersion: null,
      revertToVersionTitle: null,
      isModifyingGithubRepo: false,
      updatingNodeModules: false,
      testResults: [],
      runningTests: false
    };
  }

  /* Getters */

  getActiveDropdown(): string {
    return this.state.activeDropdown && this.state.activeDropdown.name ? this.state.activeDropdown.name : "";
  }

  otherBehaviorsInGroup(): Array<BehaviorVersion> {
    return this.getBehaviorGroup().behaviorVersions.filter(ea => ea.behaviorId !== this.getSelectedId());
  }

  getOtherSavedInputsInGroup(): Array<Input> {
    const currentBehaviorInputIds = this.getInputs().map(ea => ea.inputId);
    return this.getBehaviorGroup().getInputs().filter((ea) => {
      return ea.isSaved() && !currentBehaviorInputIds.includes(ea.inputId)
    });
  }

  getAllAWSConfigs(): Array<AWSConfigRef> {
    return this.props.awsConfigs;
  }

  getRequiredAWSConfigs(): Array<RequiredAWSConfig> {
    return this.getBehaviorGroup().getRequiredAWSConfigs();
  }

  getAllOAuthApplications(): Array<OAuthApplicationRef> {
    return this.props.oauthApplications;
  }

  getRequiredOAuthApiConfigs(): Array<RequiredOAuthApplication> {
    return this.getBehaviorGroup().getRequiredOAuthApiConfigs();
  }

  getAllSimpleTokenApis(): Array<SimpleTokenApiRef> {
    return this.props.simpleTokenApis;
  }

  getRequiredSimpleTokenApis(): Array<RequiredSimpleTokenApi> {
    return this.getBehaviorGroup().getRequiredSimpleTokenApis();
  }

  getOAuthApiApplications(): Array<RequiredOAuthApplication> {
    return this.getRequiredOAuthApiConfigs().filter(ea => !!ea.config);
  }

  getSelectedApiConfigId(): Option<string> {
    return this.state ? this.state.selectedApiConfigId : undefined;
  }

  getSelectedApiConfig(): Option<RequiredApiConfig> {
    const selectedId = this.getSelectedApiConfigId();
    return this.getRequiredApiConfigWithId(selectedId);
  }

  getAllConfigs(): Array<ApiConfigRef> {
    const configs: Array<ApiConfigRef> = [];
    return configs
      .concat(this.getAllAWSConfigs())
      .concat(this.getAllOAuthApplications())
      .concat(this.getAllSimpleTokenApis());
  }

  getAllRequiredApiConfigs(): Array<RequiredApiConfig> {
    const requiredConfigs: Array<RequiredApiConfig> = [];
    return requiredConfigs
      .concat(this.getRequiredAWSConfigs())
      .concat(this.getRequiredOAuthApiConfigs())
      .concat(this.getRequiredSimpleTokenApis());
  }

  getRequiredApiConfigWithId(id): Option<RequiredApiConfig> {
    return this.getAllRequiredApiConfigs().find(ea => ea.id === id);
  }

  getApiLogoUrlForApi(api: Option<ApiJson>): Option<string> {
    return api ? (api.logoImageUrl || api.iconImageUrl) : null;
  }

  getOAuthLogoUrlForConfig(apiId: string): string {
    const api = this.getOAuthApiWithId(apiId);
    return this.getApiLogoUrlForApi(api) || "";
  }

  getSimpleTokenLogoUrlForConfig(apiId: string): string {
    const api = this.getSimpleTokenApiWithId(apiId);
    return this.getApiLogoUrlForApi(api) || "";
  }

  getApiConfigName(config: RequiredApiConfig): string {
    const apiName = config.editorFor(this).onGetApiName(config.apiId);
    const configName = config.configName();
    if (configName && configName.toLowerCase().includes(apiName.toLowerCase())) {
      return configName;
    } else if (configName) {
      return `${apiName} — ${configName}`;
    } else {
      return apiName;
    }
  }

  getOAuthApiNameForConfig(apiId: string): string {
    const api = this.getOAuthApiWithId(apiId);
    return api ? api.name : "";
  }

  getSimpleTokenNameForConfig(apiId: string): string {
    const api = this.getSimpleTokenApiWithId(apiId);
    return api ? api.displayName : "";
  }

  getEditableName(): string {
    const selected = this.getSelected();
    return selected && selected.name || "";
  }

  getEditableDescription(): string {
    const selected = this.getSelected();
    return selected && selected.description || "";
  }

  getFunctionBody(): string {
    const selected = this.getSelected();
    return selected && selected.functionBody || "";
  }

  getInputIds(): Array<string> {
    const selected = this.getSelectedBehavior();
    return selected && selected.inputIds || [];
  }

  getInputs(): Array<Input> {
    const inputIds = this.getInputIds();
    return this.getBehaviorGroup().getInputs().filter((input) => {
      return input.inputId ? inputIds.includes(input.inputId) : false;
    });
  }

  getFirstBehaviorInputName(): string {
    const inputs = this.getInputs();
    if (inputs[0] && inputs[0].name) {
      return inputs[0].name;
    } else {
      return "";
    }
  }

  getSelectedId(): Option<string> {
    if (this.state) {
      return this.state.selectedId;
    } else {
      return this.props.selectedId;
    }
  }

  getBehaviorGroup(): BehaviorGroup {
    if (this.state) {
      return this.state.group;
    } else {
      return this.props.group;
    }
  }

  getSelectedBehavior(): Option<BehaviorVersion> {
    const selected = this.getSelected();
    return (selected && selected.isBehaviorVersion()) ? selected : null;
  }

  getSelectedFor(group: BehaviorGroup, selectedId: Option<string>): Option<Editable> {
    return group.getEditables().find(ea => {
      return ea.getPersistentId() === selectedId;
    });
  }

  getSelected(): Option<Editable> {
    return this.getSelectedFor(this.getBehaviorGroup(), this.getSelectedId());
  }

  getBehaviorTemplate(): Option<ResponseTemplate> {
    const selectedBehavior = this.getSelectedBehavior();
    if (!selectedBehavior) {
      return null;
    }
    const template = selectedBehavior.responseTemplate;
    if (!template && !this.isDataTypeBehavior()) {
      return new ResponseTemplate();
    } else {
      return template;
    }
  }

  getBehaviorTriggers(): Array<Trigger> {
    const selected = this.getSelectedBehavior();
    return selected && selected.triggers || [];
  }

  getBehaviorConfig(): Option<BehaviorConfig> {
    const selected = this.getSelectedBehavior();
    return selected ? selected.config : null;
  }

  getSystemParams(): Array<string> {
    return ["ellipsis"];
  }

  getEnvVariables(): Array<EnvironmentVariableData> {
    if (this.state) {
      return this.state.envVariables;
    } else {
      return this.getInitialEnvVariables();
    }
  }

  getEnvVariableNames(): Array<string> {
    return this.getEnvVariables().map(function(ea) {
      return ea.name;
    });
  }

  getFormAction(): string {
    return jsRoutes.controllers.BehaviorEditorController.save().url;
  }

  buildEnvVarNotifications(): Array<EnvVarMissingNotificationData> {
    const selectedBehavior = this.getSelectedBehavior();
    const existingEnvVars = this.getEnvVariables();
    if (selectedBehavior) {
      return selectedBehavior.getEnvVarNamesInFunction().filter((varName) => {
        const existingVar = existingEnvVars.find((ea) => ea.name === varName);
        return !existingVar || !existingVar.isAlreadySavedWithValue;
      }).map((varName) => new EnvVarMissingNotificationData({
        environmentVariableName: varName,
        onClick: () => {
          this.showEnvVariableSetter(varName);
        }
      }));
    } else {
      return [];
    }
  }

  getRequiredAWSConfigsWithNoMatchingAWSConfig(): Array<RequiredAWSConfig> {
    return this.getRequiredAWSConfigs().filter(ea => !ea.config);
  }

  buildAWSNotifications(): Array<RequiredAwsConfigNotificationData> {
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
  }

  getOAuthApiWithId(apiId: string): Option<OAuthApiJson> {
    return this.props.oauthApis.find(ea => ea.apiId === apiId);
  }

  getSimpleTokenApiWithId(tokenId: string): Option<SimpleTokenApiRef> {
    return this.props.simpleTokenApis.find(ea => ea.id === tokenId);
  }

  getRequiredOAuthApiConfigsWithNoApplication(): Array<RequiredOAuthApplication> {
    return this.getRequiredOAuthApiConfigs().filter(ea => !ea.config);
  }

  getOAuthApplicationsRequiringAuth(): Array<RequiredOAuthApplication> {
    return this.getOAuthApiApplications().filter(ea => {
      return ea.config && !this.props.linkedOAuthApplicationIds.includes(ea.config.id);
    });
  }

  isConfiguringApi(): boolean {
    return this.props.activePanelName === "configureApi";
  }

  buildOAuthApplicationNotifications(): Array<OAuthConfigWithoutApplicationNotificationData> {
    if (this.isConfiguringApi()) {
      return [];
    }
    return this.getRequiredOAuthApiConfigsWithNoApplication().map(ea => {
      const oauthApi = this.getOAuthApiWithId(ea.apiId);
      const name = oauthApi ? oauthApi.name : "Unknown";
      return new OAuthConfigWithoutApplicationNotificationData({
        name: name,
        requiredApiConfig: ea,
        existingOAuthApplications: this.getAllOAuthApplications(),
        onUpdateOAuthApplication: this.onUpdateOAuthApplication,
        onNewOAuthApplication: this.onNewOAuthApplication,
        onConfigClick: this.onApiConfigClick.bind(this, ea)
      });
    });
  }

  getParamTypesNeedingConfiguration(): Array<ParamType> {
    const paramTypes = Array.from(new Set(this.getInputs().map(ea => ea.paramType)));
    return paramTypes.filter((ea): ea is ParamType => ea ? ea.needsConfig : false);
  }

  buildDataTypeNotifications(): Array<NotificationData> {
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
      .filter((ea) => {
        const config = ea.getDataTypeConfig();
        return config ? config.isMissingFields() : false
      })
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

    const notifications: Array<NotificationData> = [];
    return notifications.concat(needsConfig, unnamedDataTypes, missingFields, unnamedFields, duplicateFields);
  }

  getValidParamNamesForTemplate(): Array<string> {
    return this.getInputs().map((param) => param.name)
      .concat(this.getSystemParams())
      .concat('successResult');
  }

  buildTemplateNotifications(): Array<UnknownParamInTemplateNotificationData> {
    const selectedBehavior = this.getSelectedBehavior();
    if (selectedBehavior) {
      const template = this.getBehaviorTemplate();
      const validParams = this.getValidParamNamesForTemplate();
      const unknownTemplateParams = template ? template.getUnknownParamsExcluding(validParams) : [];
      return unknownTemplateParams.map((paramName) => new UnknownParamInTemplateNotificationData({
        name: paramName
      }));
    } else {
      return [];
    }
  }

  buildServerNotifications(): Array<NotificationData> {
    if (!this.state) return [];
    const notifications: Array<NotificationData> = [];
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
  }

  buildSkillDetailsNotifications(): Array<SkillDetailsWarningNotificationData> {
    if (this.isExistingGroup() && !this.getBehaviorGroup().name) {
      return [new SkillDetailsWarningNotificationData({
        type: "no_skill_name",
        onClick: this.toggleRequestSkillDetails
      })];
    } else {
      return [];
    }
  }

  getFailingTestResults(): Array<BehaviorTestResult> {
    if (this.state) {
      return this.state.testResults.filter(ea => !ea.isPass);
    } else {
      return [];
    }
  }

  selectFirstTestFailure(): void {
    const first = this.getFailingTestResults()[0];
    if (first) {
      const matchingTest = this.getTests().find(ea => ea.id === first.behaviorVersionId);
      if (matchingTest) {
        this.onSelect(this.getBehaviorGroup().id, matchingTest.getPersistentId());
      }
    }
  }

  buildTestResultNotifications(): Array<TestResultsNotificationData> {
    if (this.getFailingTestResults().length > 0) {
      return [new TestResultsNotificationData({
        type: "test_failures",
        onClick: this.selectFirstTestFailure
      })];
    } else {
      return [];
    }
  }

  buildNotifications(): Array<NotificationData> {
    const notifications: Array<NotificationData> = [];
    return notifications.concat(
      this.buildEnvVarNotifications(),
      this.buildAWSNotifications(),
      this.buildOAuthApplicationNotifications(),
      this.buildDataTypeNotifications(),
      this.buildTemplateNotifications(),
      this.buildServerNotifications(),
      this.buildSkillDetailsNotifications(),
      this.buildTestResultNotifications()
    );
  }

  getNotifications(): Array<NotificationData> {
    return this.state.notifications;
  }

  getVersions(): Array<BehaviorGroup> {
    return this.state.versions;
  }

  getParamTypes(): Array<ParamType> {
    const customTypes = Sort.arrayAlphabeticalBy(this.getBehaviorGroup().getCustomParamTypes(), (ea) => ea.name);
    return this.props.builtinParamTypes.concat(customTypes);
  }

  getParamTypesForInput(): Array<ParamType> {
    const selectedBehaviorVersion = this.getSelected();
    const selectedBehaviorVersionId = selectedBehaviorVersion ? selectedBehaviorVersion.id : null;
    return this.getParamTypes().filter(ea => ea.id !== selectedBehaviorVersionId);
  }

  /* Setters/togglers */

  setBehaviorInputs(newBehaviorInputs: Array<Input>, callback?: () => void): void {
    const selected = this.getSelectedBehavior();
    if (selected) {
      const newGroup = this.getBehaviorGroup().copyWithInputsForBehaviorVersion(newBehaviorInputs, selected);
      this.updateGroupStateWith(newGroup, callback);
    }
  }

  addInput(input: Input, callback?: () => void): void {
    const newInputs = this.getInputs().concat([input]);
    this.setBehaviorInputs(newInputs, callback);
  }

  addNewInput(optionalNewName: Option<string>, callback?: () => void): void {
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
  }

  addTrigger(newTrigger: Trigger, callback?: () => void): void {
    this.setEditableProps<BehaviorVersionInterface>({
      triggers: this.getBehaviorTriggers().concat(newTrigger)
    }, callback);
  }

  cloneEditable(): void {
    const editable = this.getSelected();
    if (editable) {
      if (editable.isBehaviorVersion()) {
        this.addNewBehavior(editable.isDataType(), editable.isTest(), editable.behaviorId);
      } else if (editable.isLibraryVersion()) {
        this.cloneLibrary(editable.libraryId);
      }
    }
  }

  confirmDeleteEditable(): void {
    this.toggleActivePanel('confirmDeleteEditable', true);
  }

  confirmDeleteBehaviorGroup(): void {
    this.toggleActivePanel('confirmDeleteBehaviorGroup', true);
  }

  toggleConfirmUndo(): void {
    this.toggleActivePanel('confirmUndo', true);
  }

  confirmRevert(newBehaviorGroup: BehaviorGroup, newBehaviorGroupTitle: string): void {
    this.setState({
      revertToVersion: newBehaviorGroup,
      revertToVersionTitle: newBehaviorGroupTitle
    }, this.toggleConfirmRevert);
  }

  doRevert(): void {
    const newGroup = this.state.revertToVersion;
    if (newGroup) {
      this.setState({
        revertToVersion: null,
        revertToVersionTitle: null
      }, () => {
        this.onReplaceBehaviorGroup(newGroup);
      });
    }
  }

  toggleConfirmRevert(): void {
    this.toggleActivePanel('confirmRevert', true, () => {
      if (this.props.activePanelName !== 'confirmRevert') {
        this.setState({
          revertToVersion: null,
          revertToVersionTitle: null
        });
      }
    });
  }

  deleteEditable(): void {
    this.props.onClearActivePanel();
    const group = this.getBehaviorGroup();
    const updatedGroup = group.clone({
      behaviorVersions: group.behaviorVersions.filter(ea => ea.behaviorId !== this.getSelectedId()),
      libraryVersions: group.libraryVersions.filter(ea => ea.libraryId !== this.getSelectedId())
    });
    this.updateGroupStateWith(updatedGroup);
  }

  deleteBehaviorGroup(): void {
    if (this.deleteBehaviorGroupForm) {
      this.deleteBehaviorGroupForm.submit();
    }
  }

  deleteInputAtIndex(index: number): void {
    this.setEditableProps<BehaviorVersionInterface>({
      inputIds: ImmutableObjectUtils.arrayRemoveElementAtIndex(this.getInputIds(), index)
    });
  }

  deleteTriggerAtIndex(index: number): void {
    const triggers = ImmutableObjectUtils.arrayRemoveElementAtIndex(this.getBehaviorTriggers(), index);
    this.setEditableProps<BehaviorVersionInterface>({
      triggers: triggers
    });
  }

  getLeftPanelCoordinates(): Coords {
    var headerHeight = this.getHeaderHeight();
    var availableHeight = this.getAvailableHeight();
    var newHeight = availableHeight > 0 ? availableHeight : window.innerHeight;
    return {
      top: headerHeight,
      left: window.scrollX > 0 ? -window.scrollX : 0,
      bottom: newHeight
    };
  }

  hasMobileLayout(): boolean {
    return this.state.hasMobileLayout;
  }

  windowIsMobile(): boolean {
    return window.innerWidth <= MOBILE_MAX_WIDTH;
  }

  checkMobileLayout(): void {
    if (this.hasMobileLayout() !== this.windowIsMobile()) {
      this.setState({
        behaviorSwitcherVisible: !this.windowIsMobile(),
        hasMobileLayout: this.windowIsMobile()
      });
    }
  }

  layoutDidUpdate(): void {
    if (this.leftPanel) {
      this.leftPanel.resetCoordinates();
    }
  }

  getAvailableHeight(): number {
    return window.innerHeight - this.getHeaderHeight() - (this.props.activePanelIsModal ? 0 : this.props.footerHeight);
  }

  getHeaderHeight(): number {
    return this.props.headerHeight;
  }

  updateBehaviorScrollPosition(): void {
    if (this.getSelected()) {
      this.setEditableProps({
        editorScrollPosition: window.scrollY
      });
    }
  }

  loadVersions(): void {
    const groupId = this.getBehaviorGroup().id;
    if (groupId) {
      const url = jsRoutes.controllers.BehaviorEditorController.versionInfoFor(groupId).url;
      this.setState({
        versionsLoadStatus: VersionsLoadStatus.Loading
      }, () => {
        DataRequest
          .jsonGet(url)
          .then((json: Array<BehaviorGroupJson>) => {
            const versions = json.map((version) => {
              return BehaviorGroup.fromJson(version);
            });
            this.setState({
              versions: versions,
              versionsLoadStatus: VersionsLoadStatus.Loaded
            });
          }).catch(() => {
            // TODO: figure out what to do if there's a request error
            this.setState({
              versionsLoadStatus: VersionsLoadStatus.Error
            });
          });
      });
    }
  }

  handleEscKey(): void {
    if (this.getActiveDropdown()) {
      this.hideActiveDropdown();
    }
  }

  hideActiveDropdown(): void {
    this.setState({
      activeDropdown: null
    });
  }

  onDocumentClick(event: MouseEvent): void {
    if (this.getActiveDropdown() && !DropdownContainer.eventIsFromDropdown(event)) {
      this.hideActiveDropdown();
    }
  }

  onDocumentKeyDown(event: KeyboardEvent): void {
    if (Event.keyPressWasEsc(event)) {
      this.handleEscKey();
    } else if (Event.keyPressWasSaveShortcut(event)) {
      event.preventDefault();
      if (this.isModified()) {
        this.onSaveBehaviorGroup();
      }
    }
  }

  onSaveError(error?: Option<ResponseError>, message?: Option<string>): void {
    this.props.onClearActivePanel();
    this.setState({
      error: error && error.body || message || "not_saved",
      updatingNodeModules: false,
      runningTests: false
    });
  }

  onDeployError(error?: Option<ResponseError>, callback?: () => void): void {
    this.setState({
      error: error && error.body || "not_deployed"
    }, callback);
    this.resetNotificationsImmediately();
  }

  deploy(callback?: () => void): void {
    this.setState({ error: null });
    DataRequest.jsonPost(
      jsRoutes.controllers.BehaviorEditorController.deploy().url,
      { behaviorGroupId: this.getBehaviorGroup().id },
      this.props.csrfToken
    )
      .then((json: BehaviorGroupDeploymentJson) => {
        if (json.id) {
          this.props.onDeploy(json, callback);
        } else {
          this.onDeployError(null, callback);
        }
      })
      .catch((error?: Option<ResponseError>) => {
        this.onDeployError(error, callback);
      });
  }

  updateNodeModules(optionalCallback?: () => void): void {
    this.setState({
      error: null,
      updatingNodeModules: true
    });
    this.toggleActivePanel('saving', true);
    DataRequest.jsonPost(
      jsRoutes.controllers.BehaviorEditorController.updateNodeModules().url,
      { behaviorGroupId: this.getBehaviorGroup().id },
      this.props.csrfToken
    )
      .then((json: BehaviorGroupJson) => {
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
      .catch((error?: Option<ResponseError>) => {
        this.onSaveError(error);
      });
  }

  backgroundSave(optionalCallback?: () => void): void {
    this.setState({
      newerVersionOnServer: null,
      errorReachingServer: null
    }, () => this.doBackgroundSave(optionalCallback));
  }

  doBackgroundSave(optionalCallback?: () => void) {
    DataRequest.jsonPost(this.getFormAction(), {
      dataJson: JSON.stringify(this.getBehaviorGroup())
    }, this.props.csrfToken)
      .then((json: BehaviorGroup) => {
        if (json.id) {
          const group = this.getBehaviorGroup();
          const teamId = group.teamId;
          const groupId = json.id;
          if (this.state.shouldRedirectToAddNewOAuthApp) {
            const config = this.state.requiredOAuthApiConfig;
            window.location.href = jsRoutes.controllers.web.settings.IntegrationsController.add(teamId, groupId, this.getSelectedId(), config ? config.nameInCode : null).url;
          } else if (this.state.shouldRedirectToAddNewAWSConfig) {
            const config = this.state.requiredAWSConfig;
            window.location.href = jsRoutes.controllers.web.settings.AWSConfigController.add(teamId, groupId, this.getSelectedId(), config ? config.nameInCode: null).url;
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
      .catch((error: ResponseError) => {
        this.onSaveError(error);
      });
  }

  isJustSaved(): boolean {
    return this.getBehaviorGroup().isRecentlySaved() && !this.isModified();
  }

  onSaveClick(): void {
    this.onSaveBehaviorGroup();
  }

  onSaveBehaviorGroup(optionalCallback?: () => void): void {
    this.setState({ error: null }, () => {
      this.toggleActivePanel('saving', true, () => {
        this.backgroundSave(optionalCallback);
      });
    });
  }

  onReplaceBehaviorGroup(newBehaviorGroup: BehaviorGroup): void {
    const selectedId = this.getSelectedId();
    const newSelectedId = selectedId && newBehaviorGroup.hasBehaviorVersionWithId(selectedId) ? selectedId : undefined;
    const newState = {
      group: newBehaviorGroup,
      selectedId: newSelectedId
    };
    this.setState(newState, this.onSaveBehaviorGroup);
  }

  setEditableProps<T extends EditableInterface>(props: Partial<T>, callback?: () => void) {
    const existingGroup = this.getBehaviorGroup();
    const existingSelected = this.getSelectedFor(existingGroup, this.getSelectedId());
    if (existingSelected) {
      const updatedGroup = existingSelected.buildUpdatedGroupFor(existingGroup, props);
      this.updateGroupStateWith(updatedGroup, callback);
    }
  }

  getNextBehaviorIdFor(group) {
    if (group.behaviorVersions.length) {
      return group.behaviorVersions[0].behaviorId;
    } else {
      return null;
    }
  }

  updateGroupStateWith(updatedGroup: BehaviorGroup, callback?: () => void): void {
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
  }

  setBehaviorConfigProps(props: Partial<BehaviorConfigInterface>): void {
    const config = this.getBehaviorConfig();
    if (config) {
      this.setEditableProps<BehaviorVersionInterface>({
        config: config.clone(props)
      });
    }
  }

  showEnvVariableSetter(nameToFocus?: Option<string>): void {
    this.toggleActivePanel('envVariableSetter', true, () => {
      if (nameToFocus && this.envVariableSetterPanel) {
        this.envVariableSetterPanel.focusOrCreateVarName(nameToFocus);
      }
    });
  }

  showVersions(): void {
    if (!this.versionsMaybeLoaded()) {
      this.loadVersions();
    }
    this.toggleActivePanel('versionBrowser', false, this.updateVersionBrowserOpenState);
  }

  updateVersionBrowserOpenState(): void {
    this.setState({
      versionBrowserOpen: this.props.activePanelName === 'versionBrowser'
    }, () => {
      if (this.state.versionBrowserOpen) {
        BrowserUtils.replaceQueryParam("showVersions", "true");
      } else {
        BrowserUtils.removeQueryParam("showVersions");
      }
    });
  }

  toggleActiveDropdown(name: string): void {
    var alreadyOpen = this.getActiveDropdown() === name;
    this.setState({
      activeDropdown: alreadyOpen ? null : { name: name }
    });
  }

  toggleGroupEditorIconPicker(): void {
    this.toggleActiveDropdown("behaviorGroupEditorIconPicker");
  }

  toggleDetailsPanelIconPicker(): void {
    this.toggleActiveDropdown("behaviorGroupDetailsPanelIconPicker");
  }

  toggleActivePanel(name: string, beModal?: boolean, optionalCallback?: () => void): void {
    this.props.onToggleActivePanel(name, beModal, optionalCallback);
  }

  toggleChangeGithubRepo(): void {
    this.setState({
      isModifyingGithubRepo: !this.state.isModifyingGithubRepo
    });
  }

  toggleSharedAnswerInputSelector(): void {
    this.toggleActivePanel('sharedAnswerInputSelector', true);
  }

  toggleRequestSkillDetails(): void {
    this.toggleActivePanel('requestBehaviorGroupDetails', true);
  }

  toggleBehaviorSwitcher(): void {
    this.setState({
      behaviorSwitcherVisible: !this.state.behaviorSwitcherVisible
    });
  }

  checkIfModifiedAndTest(): void {
    const ref = this.isDataTypeBehavior() ? 'dataTypeTester' : 'behaviorTester';
    if (this.isModified()) {
      this.onSaveBehaviorGroup(() => {
        this.props.onClearActivePanel(() => {
          this.toggleActivePanel(ref, true);
        });
      });
    } else {
      this.props.onClearActivePanel(() => {
        this.toggleActivePanel(ref, true);
      });
    }
  }

  toggleCodeEditorLineWrapping(): void {
    this.setState({
      codeEditorUseLineWrapping: !this.state.codeEditorUseLineWrapping
    });
  }

  toggleResponseTemplateHelp(): void {
    this.toggleActivePanel('helpForResponseTemplate');
  }

  toggleDevModeChannelsHelp(): void {
    this.toggleActivePanel('helpForDevModeChannels');
  }

  toggleSavedAnswerEditor(savedAnswerId?: string): void {
    if (this.props.activePanelName === 'savedAnswerEditor') {
      this.toggleActivePanel('savedAnswerEditor', true, () => {
        this.setState({ selectedSavedAnswerInputId: null });
      });
    } else {
      this.setState({ selectedSavedAnswerInputId: savedAnswerId }, () => {
        this.toggleActivePanel('savedAnswerEditor', true, () => null);
      });
    }
  }

  toggleUserInputHelp(): void {
    this.toggleActivePanel('helpForUserInput');
  }

  toggleTriggerHelp(): void {
    this.toggleActivePanel('helpForTriggerParameters');
  }

  updateCode(newCode: string): void {
    this.setEditableProps({
      functionBody: newCode
    });
  }

  onChangeCanBeMemoized(bool: boolean): void {
    this.setBehaviorConfigProps({
      canBeMemoized: bool
    });
  }

  updateDataType(optionalNewConfig?: Option<Partial<BehaviorConfigInterface>>, optionalNewCode?: Option<string>, optionalCallback?: () => void) {
    const existingConfig = this.getBehaviorConfig();
    const withConfig = optionalNewConfig && existingConfig ? { config: existingConfig.clone(optionalNewConfig) } : null;
    const withCode = typeof optionalNewCode === "string" ? { functionBody: optionalNewCode } : null;
    const props = Object.assign({}, withConfig, withCode);
    this.setEditableProps(props, optionalCallback);
  }

  updateDescription(newDescription: string): void {
    this.setEditableProps({
      description: newDescription
    });
  }

  updateName(newName: string): void {
    const normalizedName = this.isDataTypeBehavior() ? Formatter.formatDataTypeName(newName) : newName;
    this.setEditableProps({
      name: normalizedName
    });
  }

  updateEnvVariables(envVars: Array<EnvironmentVariableData>): void {
    var url = jsRoutes.controllers.web.settings.EnvironmentVariablesController.submit().url;
    var data = {
      teamId: this.getBehaviorGroup().teamId,
      variables: envVars
    };
    DataRequest.jsonPost(url, {
      teamId: this.getBehaviorGroup().teamId,
      dataJson: JSON.stringify(data)
    }, this.props.csrfToken)
      .then((json: EnvironmentVariablesData) => {
        this.props.onClearActivePanel();
        this.setState({
          envVariables: json.variables
        }, () => {
          this.resetNotificationsImmediately();
          if (this.envVariableSetterPanel) {
            this.envVariableSetterPanel.reset();
          }
        });
      }).catch(() => {
        if (this.envVariableSetterPanel) {
          this.envVariableSetterPanel.onSaveError();
        }
      });
  }

  deleteEnvVariable(name: string): void {
    this.setState({
      envVariables: this.state.envVariables.filter((ea) => ea.name !== name)
    });
  }

  loadAdminEnvVariableValue(name: string, value?: Option<string>): void {
    this.setState({
      envVariables: this.state.envVariables.map((ea) => {
        if (ea.name === name) {
          return {
            name: name,
            value: value,
            isAlreadySavedWithValue: Boolean(value)
          };
        } else {
          return ea;
        }
      })
    });
  }

  forgetSavedAnswerRequest(url: string, inputId: string): void {
    DataRequest.jsonPost(url, {
      inputId: inputId
    }, this.props.csrfToken)
      .then((json: { numDeleted: number }) => {
        if (json.numDeleted > 0) {
          this.props.onForgetSavedAnswerForInput(inputId, json.numDeleted);
        }
      });
  }

  forgetSavedAnswerForUser(inputId: string): void {
    const url = jsRoutes.controllers.SavedAnswerController.resetForUser().url;
    this.forgetSavedAnswerRequest(url, inputId);
  }

  forgetSavedAnswersForTeam(inputId: string): void {
    const url = jsRoutes.controllers.SavedAnswerController.resetForTeam().url;
    this.forgetSavedAnswerRequest(url, inputId);
  }

  onBehaviorGroupNameChange(name: string): void {
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ name: name }));
  }

  onBehaviorGroupDescriptionChange(desc: string): void {
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ description: desc  }));
  }

  onBehaviorGroupIconChange(icon: string): void {
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ icon: icon  }));
  }

  updateBehaviorInputAtIndexWith(index: number, newInput: Input) {
    const oldInputs = this.getInputs();
    const oldInputName = oldInputs[index].name;
    const newInputName = newInput.name;
    const newInputs = ImmutableObjectUtils.arrayWithNewElementAtIndex(oldInputs, newInput, index);

    this.setBehaviorInputs(newInputs, () => {
      let numTriggersReplaced = 0;
      if (oldInputName === this.state.paramNameToSync) {
        numTriggersReplaced = this.syncParamNamesAndCount(oldInputName, newInputName);
        if (numTriggersReplaced > 0) {
          this.setState({ paramNameToSync: newInputName });
        }
      }
    });
  }

  moveBehaviorInputAtIndex(index: number, newIndex: number): void {
    const oldInputs = this.getInputs();
    this.setBehaviorInputs(ImmutableObjectUtils.arrayMoveElement(oldInputs, index, newIndex));
  }

  updateTemplate(newTemplateString: string): void {
    const template = this.getBehaviorTemplate();
    if (template) {
      this.setEditableProps<BehaviorVersionInterface>({
        responseTemplate: template.clone({ text: newTemplateString })
      });
    }
  }

  onSelectResponseType(newValue: string): void {
    this.setBehaviorConfigProps({
      responseTypeId: newValue
    });
  }

  syncParamNamesAndCount(oldName: string, newName: string): number {
    let numTriggersModified = 0;

    const newTriggers = this.getBehaviorTriggers().map((oldTrigger) => {
      if (oldTrigger.usesInputName(oldName)) {
        numTriggersModified++;
        return oldTrigger.clone({ text: oldTrigger.getTextWithNewInputName(oldName, newName) });
      } else {
        return oldTrigger;
      }
    });

    const oldTemplate = this.getBehaviorTemplate();
    const newTemplate = oldTemplate ? oldTemplate.replaceParamName(oldName, newName) : null;
    const templateModified = oldTemplate && newTemplate && newTemplate.text !== oldTemplate.text;

    const newProps: Partial<BehaviorVersionInterface> = {};
    if (numTriggersModified > 0) {
      newProps.triggers = newTriggers;
    }
    if (newTemplate && templateModified) {
      newProps.responseTemplate = newTemplate;
    }
    if (Object.keys(newProps).length > 0) {
      this.setEditableProps(newProps);
    }

    return numTriggersModified + (templateModified ? 1 : 0);
  }

  updateTriggerAtIndexWithTrigger(index: number, newTrigger: Trigger) {
    this.setEditableProps<BehaviorVersionInterface>({
      triggers: ImmutableObjectUtils.arrayWithNewElementAtIndex(this.getBehaviorTriggers(), newTrigger, index)
    });
  }

  undoChanges(): void {
    this.updateGroupStateWith(this.props.group, () => {
      this.props.onClearActivePanel();
      this.resetNotificationsEventually();
    });
  }

  /* Booleans */

  isTestable(): boolean {
    const selected = this.getSelectedBehavior();
    return Boolean(selected && selected.usesCode() && !selected.isTest());
  }

  getActionBehaviors(): Array<BehaviorVersion> {
    return this.getBehaviorGroup().getActions();
  }

  getDataTypeBehaviors(): Array<BehaviorVersion> {
    return this.getBehaviorGroup().getDataTypes();
  }

  getLibraries(): Array<LibraryVersion> {
    return this.getBehaviorGroup().libraryVersions;
  }

  getTests(): Array<BehaviorVersion> {
    return this.getBehaviorGroup().getTests();
  }

  getNodeModuleVersions(): Array<NodeModuleVersion> {
    return Sort.arrayAlphabeticalBy(this.state.nodeModuleVersions || [], (ea) => ea.from);
  }

  hasInputNamed(name: string): boolean {
    return this.getInputs().some(ea => ea.name === name);
  }

  isDataTypeBehavior(): boolean {
    const selected = this.getSelectedBehavior();
    return Boolean(selected && selected.isDataType());
  }

  // TODO: This is an obsolete concept
  isSearchDataTypeBehavior(): boolean {
    return this.isDataTypeBehavior() && this.hasInputNamed('searchQuery');
  }

  isExistingEditable(): boolean {
    const selected = this.getSelected();
    return Boolean(selected && !selected.isNew);
  }

  isExistingGroup(): boolean {
    return this.getBehaviorGroup().isExisting();
  }

  isModified(): boolean {
    const currentMatchesInitial = this.props.group.isIdenticalTo(this.getBehaviorGroup());
    return !currentMatchesInitial;
  }

  editableIsModified(current: Editable): boolean {
    var original = this.props.group.getEditables().find((ea) => ea.getPersistentId() === current.getPersistentId());
    return !(original && current.isIdenticalToVersion(original));
  }

  isSaving(): boolean {
    return this.props.activePanelName === 'saving';
  }

  versionsMaybeLoaded(): boolean {
    return this.state.versionsLoadStatus === 'loading' || this.state.versionsLoadStatus === 'loaded';
  }

  /* Interaction and event handling */

  ensureCursorVisible(newPosition: EditorCursorPosition): void {
    const height = this.props.footerHeight;
    if (!height) {
      return;
    }
    BrowserUtils.ensureYPosInView(newPosition.bottom, height);
  }

  onAddAWSConfig(toAdd: RequiredAWSConfig, callback?: () => void): void {
    const existing = this.getRequiredAWSConfigs();
    const newConfigs = existing.concat([toAdd]);
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ requiredAWSConfigs: newConfigs }), callback);
  }

  onRemoveAWSConfig(config: RequiredAWSConfig, callback?: () => void): void {
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
  }

  onUpdateAWSConfig(config: RequiredAWSConfig, callback?: () => void): void {
    const configs = this.getRequiredAWSConfigs().slice();
    const indexToReplace = configs.findIndex(ea => ea.id === config.id);
    configs[indexToReplace] = config;
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ requiredAWSConfigs: configs }), callback);
  }

  addNewAWSConfig(required?: RequiredAWSConfig): void {
    const requiredToUse = required || RequiredAWSConfig.fromProps({
      id: ID.next(),
      exportId: null,
      apiId: 'aws',
      nameInCode: "",
      config: null
    });
    this.onNewAWSConfig(requiredToUse);
  }

  onAddOAuthApplication(toAdd: RequiredOAuthApplication, callback?: () => void) {
    const existing = this.getRequiredOAuthApiConfigs();
    const newApplications = existing.concat([toAdd]);
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ requiredOAuthApiConfigs: newApplications }), callback);
  }

  onRemoveOAuthApplication(toRemove: RequiredOAuthApplication, callback?: () => void): void {
    const existing = this.getRequiredOAuthApiConfigs();
    const newApplications = existing.filter(function(ea) {
      return ea.id !== toRemove.id;
    });
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ requiredOAuthApiConfigs: newApplications }), () => {
      this.props.onClearActivePanel();
      if (callback) {
        callback();
      }
    });
  }

  onUpdateOAuthApplication(config: RequiredOAuthApplication, callback?: () => void): void {
    const configs = this.getRequiredOAuthApiConfigs().slice();
    const indexToReplace = configs.findIndex(ea => ea.id === config.id);
    configs[indexToReplace] = config;
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ requiredOAuthApiConfigs: configs }), callback);
  }

  onAddNewRequiredConfig(required: RequiredApiConfig): void {
    this.selectRequiredApiConfig(required);
  }

  addNewOAuthApplication(required?: RequiredOAuthApplication): void {
    const requiredToUse = required || RequiredOAuthApplication.fromProps({
      id: ID.next(),
      exportId: null,
      apiId: "",
      nameInCode: "",
      config: null,
      recommendedScope: ""
    });
    this.onNewOAuthApplication(requiredToUse);
  }

  onAddSimpleTokenApi(toAdd: RequiredSimpleTokenApi): void {
    const newConfigs = this.getRequiredSimpleTokenApis().concat([toAdd]);
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ requiredSimpleTokenApis: newConfigs }));
  }

  onRemoveSimpleTokenApi(toRemove: RequiredSimpleTokenApi): void {
    const newConfigs = this.getRequiredSimpleTokenApis().filter(function(ea) {
      return ea.apiId !== toRemove.apiId;
    });
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ requiredSimpleTokenApis: newConfigs }));
  }

  onUpdateSimpleTokenApi(config: RequiredSimpleTokenApi, callback?: () => void): void {
    const configs = this.getRequiredSimpleTokenApis().slice();
    const indexToReplace = configs.findIndex(ea => ea.id === config.id);
    configs[indexToReplace] = config;
    this.updateGroupStateWith(this.getBehaviorGroup().clone({ requiredSimpleTokenApis: configs }), callback);
  }

  onNewAWSConfig(requiredAWSConfig: RequiredAWSConfig): void {
    this.setState({
      shouldRedirectToAddNewAWSConfig: true,
      requiredAWSConfig: requiredAWSConfig
    }, this.onSaveBehaviorGroup);
  }

  onNewOAuthApplication(requiredOAuthApiConfig: RequiredOAuthApplication): void {
    this.setState({
      shouldRedirectToAddNewOAuthApp: true,
      requiredOAuthApiConfig: requiredOAuthApiConfig
    }, this.onSaveBehaviorGroup);
  }

  onConfigureType(paramTypeId: string): void {
    const typeBehaviorVersion = this.getBehaviorGroup().behaviorVersions.find(ea => ea.id === paramTypeId);
    if (typeBehaviorVersion) {
      this.onSelect(this.getBehaviorGroup().id, typeBehaviorVersion.behaviorId);
    }
  }

  onInputNameFocus(index: number): void {
    this.setState({
      paramNameToSync: this.getInputs()[index].name
    });
  }

  onInputNameBlur(): void {
    this.setState({
      paramNameToSync: null
    });
  }

  onSave(newProps: { group: BehaviorGroup, onLoad?: Option<() => void> }): void {
    this.props.onSave(newProps);
    this.loadNodeModuleVersions();
    this.loadTestResults();
  }

  resetNotificationsImmediately(): void {
    this.setState({
      notifications: this.buildNotifications()
    });
  }

  loadNodeModuleVersions(): void {
    const groupId = this.getBehaviorGroup().id;
    if (groupId) {
      this.setState({
        updatingNodeModules: true
      }, () => {
        DataRequest.jsonGet(jsRoutes.controllers.BehaviorEditorController.nodeModuleVersionsFor(groupId).url)
          .then((json: Array<NodeModuleVersionJson>) => {
            this.setState({
              nodeModuleVersions: NodeModuleVersion.allFromJson(json),
              updatingNodeModules: false
            });
          })
          .catch(() => {
            this.setState({
              updatingNodeModules: false
            });
          });
      });
    }
  }

  loadTestResults(): void {
    this.resetNotificationsEventually();
    const behaviorGroupId = this.getBehaviorGroup().id;
    if (behaviorGroupId) {
      this.setState({
        error: null,
        runningTests: true,
        testResults: []
      }, () => {
        this.doTestResultRequest(behaviorGroupId, 0);
      });
    }
  }

  doTestResultRequest(behaviorGroupId: string, numRetries: number): void {
    const MAX_RETRY_COUNT = 4;
    DataRequest.jsonGet(jsRoutes.controllers.BehaviorEditorController.testResults(behaviorGroupId).url)
      .then((json: BehaviorTestResultsJson) => {
        if (json.shouldRetry) {
          if (numRetries < MAX_RETRY_COUNT) {
            const newRetryCount = numRetries + 1;
            setTimeout(() => {
              this.doTestResultRequest(behaviorGroupId, newRetryCount);
            }, Math.pow(2, newRetryCount) * 1000);
          } else {
            this.onSaveError(null, "Unable to load test results. Try reloading the page in a moment.");
          }
        } else if (json.results) {
          this.setState({
            testResults: BehaviorTestResult.allFromJson(json.results),
            runningTests: false
          }, this.resetNotificationsImmediately);
        } else {
          this.onSaveError(null, "Invalid test data returned from the server");
        }
      })
      .catch((error: ResponseError) => {
        this.onSaveError(error);
      });
  }

    /* Component API methods */

  componentWillMount(): void {
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
  }

  componentDidMount(): void {
    window.document.addEventListener('click', this.onDocumentClick, false);
    window.document.addEventListener('keydown', this.onDocumentKeyDown, false);
    window.addEventListener('resize', this.checkMobileLayout, false);
    window.addEventListener('scroll', debounce(this.updateBehaviorScrollPosition, 500), false);
    window.addEventListener('focus', () => this.checkForUpdatesLater(2000), false);
    this.checkForUpdatesLater();
    this.loadNodeModuleVersions();
    this.loadTestResults();
    if (this.props.showVersions) {
      this.showVersions();
    }
    this.renderNavItems();
    this.renderNavActions();
  }

  componentDidUpdate(): void {
    this.renderNavItems();
    this.renderNavActions();
  }

  checkForUpdates(): void {
    const groupId = this.getBehaviorGroup().id;
    if (document.hasFocus() && groupId && !this.isSaving()) {
      DataRequest.jsonGet(jsRoutes.controllers.BehaviorEditorController.metaData(groupId).url)
        .then((json: BehaviorGroupVersionMetaData) => {
          if (!json.createdAt) {
            throw new Error("Invalid response");
          }
          const serverDate = new Date(json.createdAt);
          const savedDate = this.props.group.createdAt ? new Date(this.props.group.createdAt) : null;
          const isNewerVersion = !savedDate || serverDate > savedDate;
          const wasOldError = this.state.errorReachingServer;
          if (this.state.newerVersionOnServer || isNewerVersion || wasOldError) {
            this.setState({
              newerVersionOnServer: isNewerVersion ? BehaviorGroupVersionMetaData.fromJson(json) : null,
              errorReachingServer: null
            }, this.resetNotificationsEventually);
          }
          this.checkForUpdatesLater();
        })
        .catch((err: ResponseError) => {
          this.setState({
            errorReachingServer: err
          }, this.resetNotificationsEventually);
          this.checkForUpdatesLater();
        });
    } else {
      this.checkForUpdatesLater();
    }
  }

  checkForUpdatesLater(overrideDuration?: number): void {
    clearTimeout(this.checkForUpdateTimer);
    this.checkForUpdateTimer = setTimeout(this.checkForUpdates, overrideDuration || 30000);
  }

  getInitialEnvVariables(): Array<EnvironmentVariableData> {
    return Sort.arrayAlphabeticalBy(this.props.envVariables || [], (variable) => variable.name);
  }

  componentWillReceiveProps(nextProps: Props): void {
    if (nextProps.group !== this.props.group) {
      const newGroup = nextProps.group;
      const newState = {
        group: newGroup,
        versions: [],
        versionsLoadStatus: VersionsLoadStatus.None,
        error: null
      };
      this.props.onClearActivePanel();
      this.setState(newState, this.resetNotificationsImmediately);
      if (typeof(nextProps.onLoad) === 'function') {
        nextProps.onLoad();
      }
      if (newGroup.id) {
        BrowserUtils.replaceURL(jsRoutes.controllers.BehaviorEditorController.edit(newGroup.id, this.getSelectedId()).url);
      }
    }
  }

  renderCodeEditor(codeConfigProps: CodeConfigProps) {
    return (
      <CodeConfiguration
        availableHeight={this.getAvailableHeight()}
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
        oauthApiApplications={this.getOAuthApiApplications()}
        libraries={this.getLibraries()}
        nodeModules={this.getNodeModuleVersions()}

        functionBody={this.getFunctionBody()}
        onChangeFunctionBody={this.updateCode}
        onChangeCanBeMemoized={this.onChangeCanBeMemoized}
        isMemoizationEnabled={codeConfigProps.isMemoizationEnabled}
        onCursorChange={this.ensureCursorVisible}
        useLineWrapping={this.state.codeEditorUseLineWrapping}
        onToggleCodeEditorLineWrapping={this.toggleCodeEditorLineWrapping}

        envVariableNames={this.getEnvVariableNames()}
        functionExecutesImmediately={codeConfigProps.functionExecutesImmediately || false}
      />
    );
  }

  getSelectedTestResult(): Option<BehaviorTestResult> {
    const selected = this.getSelectedBehavior();
    return selected && selected.id ? this.state.testResults.find(ea => ea.behaviorVersionId === selected.id) : null;
  }

  confirmDeleteText(): string {
    const selected = this.getSelected();
    return selected ? selected.confirmDeleteText() : "";
  }

  confirmRevertText() {
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
  }

  toggleApiAdderDropdown(): void {
    this.toggleActiveDropdown("apiConfigAdderDropdown");
  }

  renderFooter() {
    const footerClassName = this.mobileBehaviorSwitcherIsVisible() ? "mobile-position-behind-scrim" : "";
    const behaviorGroupId = this.getBehaviorGroup().id;
    const selectedBehaviorId = this.getSelectedId();
    const behaviorVersion = this.getSelectedBehavior();
    const selectedApiConfig = this.getSelectedApiConfig();
    const selectedSavedAnswerInput = this.state.selectedSavedAnswerInputId ? this.getInputs().find((ea) => ea.inputId === this.state.selectedSavedAnswerInputId) : null;
    return this.props.onRenderFooter((
      <div>
          <ModalScrim isActive={this.mobileBehaviorSwitcherIsVisible()} />
          {behaviorVersion && this.isDataTypeBehavior() ? (
            <div>
              <Collapsible ref={(el) => this.props.onRenderPanel("addDataStorageItems", el)} revealWhen={this.props.activePanelName === 'addDataStorageItems'} onChange={this.layoutDidUpdate}>
                <DefaultStorageAdder
                  csrfToken={this.props.csrfToken}
                  behaviorVersion={behaviorVersion}
                  onCancelClick={this.props.onClearActivePanel}
                />
              </Collapsible>

              {behaviorGroupId ? (
                <Collapsible ref={(el) => this.props.onRenderPanel("browseDataStorage", el)} revealWhen={this.props.activePanelName === 'browseDataStorage'} onChange={this.layoutDidUpdate}>
                  <DefaultStorageBrowser
                    csrfToken={this.props.csrfToken}
                    behaviorVersion={behaviorVersion}
                    behaviorGroupId={behaviorGroupId}
                    onCancelClick={this.props.onClearActivePanel}
                    isVisible={this.props.activePanelName === 'browseDataStorage'}
                  />
                </Collapsible>
              ) : null}
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
              iconPickerVisible={this.getActiveDropdown() === 'behaviorGroupDetailsPanelIconPicker'}
              onToggleIconPicker={this.toggleDetailsPanelIconPicker}
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
              requiredConfig={selectedApiConfig}
              onDoneClick={this.props.onClearActivePanel}
              addNewAWSConfig={this.addNewAWSConfig}
              addNewOAuthApplication={this.addNewOAuthApplication}
              animationDisabled={this.state.animationDisabled}
              allConfigs={this.getAllConfigs()}
              editor={this}
              onAddNewConfig={this.onAddNewRequiredConfig}
            />
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
                      onDelete={this.deleteEnvVariable}
                      activePanelName={this.props.activePanelName}
                      activePanelIsModal={false}
                      teamId={this.getBehaviorGroup().teamId}
                      csrfToken={this.props.csrfToken}
                      isAdmin={this.props.isAdmin}
                      onAdminLoadedValue={this.loadAdminEnvVariableValue}
                      onToggleActivePanel={this.props.onToggleActivePanel}
                      onClearActivePanel={this.props.onClearActivePanel}
                    />
                  </div>
                </div>
              </div>
            </div>
          </Collapsible>

          <Collapsible revealWhen={this.props.activePanelName === 'behaviorTester'} onChange={this.layoutDidUpdate}>
            {behaviorGroupId && selectedBehaviorId ? (
              <BehaviorTester
                ref={(el) => this.props.onRenderPanel("behaviorTester", el)}
                triggers={this.getBehaviorTriggers()}
                inputs={this.getInputs()}
                groupId={behaviorGroupId}
                behaviorId={selectedBehaviorId}
                csrfToken={this.props.csrfToken}
                onDone={this.props.onClearActivePanel}
                appsRequiringAuth={this.getOAuthApplicationsRequiringAuth()}
              />
            ) : null}
          </Collapsible>

          {behaviorGroupId && selectedBehaviorId ? (
            <Collapsible revealWhen={this.props.activePanelName === 'dataTypeTester'} onChange={this.layoutDidUpdate}>
              <DataTypeTester
                ref={(el) => this.props.onRenderPanel("dataTypeTester", el)}
                groupId={behaviorGroupId}
                behaviorId={selectedBehaviorId}
                isSearch={this.isSearchDataTypeBehavior()}
                csrfToken={this.props.csrfToken}
                onDone={this.props.onClearActivePanel}
                appsRequiringAuth={this.getOAuthApplicationsRequiringAuth()}
              />
            </Collapsible>
          ) : null}

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
            {selectedSavedAnswerInput ? (
              <SavedAnswerEditor
                onToggle={this.toggleSavedAnswerEditor}
                savedAnswers={this.props.savedAnswers}
                selectedInput={selectedSavedAnswerInput}
                onForgetSavedAnswerForUser={this.forgetSavedAnswerForUser}
                onForgetSavedAnswersForTeam={this.forgetSavedAnswersForTeam}
              />
            ) : null}
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
                      disabledWhen={!this.isExistingEditable() && !this.isModified()}
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
  }

  renderFooterStatus() {
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
  }

  renderHiddenForms() {
    return (
      <div>
        <form ref={(el) => this.deleteBehaviorGroupForm = el} action={jsRoutes.controllers.ApplicationController.deleteBehaviorGroups().url} method="POST">
          <CsrfTokenHiddenInput value={this.props.csrfToken} />
          <input type="hidden" name="behaviorGroupIds[0]" value={this.getBehaviorGroup().id || ""} />
        </form>
      </div>
    );
  }

  behaviorSwitcherIsVisible(): boolean {
    return this.state.behaviorSwitcherVisible;
  }

  mobileBehaviorSwitcherIsVisible(): boolean {
    return this.hasMobileLayout() && this.behaviorSwitcherIsVisible();
  }

  renderSwitcherToggle() {
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
    } else {
      return null;
    }
  }

  getEditorScrollPosition(): number {
    const selected = this.getSelected();
    return selected && selected.editorScrollPosition || 0;
  }

  onSelect(optionalGroupId: Option<string>, optionalBehaviorId: Option<string>, optionalCallback?: () => void) {
    const newState = Object.assign({
      animationDisabled: true,
      selectedId: optionalBehaviorId
    }, this.windowIsMobile() ? {
      behaviorSwitcherVisible: false
    } : null);
    this.setState(newState, () => {
      if (optionalGroupId) {
        BrowserUtils.replaceURL(jsRoutes.controllers.BehaviorEditorController.edit(optionalGroupId, optionalBehaviorId).url);
      }
      window.scrollTo(window.scrollX, this.getEditorScrollPosition());
      this.setState({
        animationDisabled: false
      });
      if (optionalCallback) {
        optionalCallback();
      }
    });
  }

  animationIsDisabled(): boolean {
    return this.state.animationDisabled;
  }

  addNewBehavior(isDataType: boolean, isTest: boolean, behaviorIdToClone: Option<string>, optionalDefaultProps?: Partial<BehaviorVersionInterface>): void {
    const group = this.getBehaviorGroup();
    const newName = optionalDefaultProps ? optionalDefaultProps.name : null;
    const url = jsRoutes.controllers.BehaviorEditorController.newUnsavedBehavior(isDataType, isTest, group.teamId, behaviorIdToClone, newName).url;
    DataRequest.jsonGet(url)
      .then((json: BehaviorVersionJson) => {
        const newVersion = BehaviorVersion.fromJson(Object.assign({}, json, { groupId: group.id })).clone(optionalDefaultProps || {});
        const groupWithNewBehavior = group.withNewBehaviorVersion(newVersion);
        this.updateGroupStateWith(groupWithNewBehavior, () => {
          this.onSelect(groupWithNewBehavior.id, newVersion.behaviorId);
        });
      });
  }

  addNewAction(): void {
    const nextActionName = SequentialName.nextFor(this.getActionBehaviors(), (ea: BehaviorVersion) => ea.getName(), "action");
    this.addNewBehavior(false, false, null, BehaviorVersion.defaultActionProps(nextActionName));
  }

  addNewDataType(): void {
    const nextDataTypeName = SequentialName.nextFor(this.getDataTypeBehaviors(), (ea: BehaviorVersion) => ea.getName(), "DataType");
    this.addNewBehavior(true, false, null, { name: nextDataTypeName });
  }

  addNewTest(): void {
    const nextDataTypeName = SequentialName.nextFor(this.getDataTypeBehaviors(), (ea: BehaviorVersion) => ea.getName(), "test");
    this.addNewBehavior(false, true, null, { name: nextDataTypeName });
  }

  addNewLibraryImpl(libraryIdToClone: Option<string>, optionalProps?: Partial<LibraryVersionInterface>): void {
    const group = this.getBehaviorGroup();
    const url = jsRoutes.controllers.BehaviorEditorController.newUnsavedLibrary(group.teamId, libraryIdToClone).url;
    DataRequest.jsonGet(url)
      .then((json: LibraryVersionJson) => {
        const newVersion = LibraryVersion.fromJson(Object.assign({}, json, { groupId: group.id })).clone(optionalProps || {});
        const groupWithNewLibrary = group.withNewLibraryVersion(newVersion);
        this.updateGroupStateWith(groupWithNewLibrary, () => {
          this.onSelect(groupWithNewLibrary.id, newVersion.libraryId);
        });
      });
  }

  addNewLibrary(): void {
    const nextLibraryName = SequentialName.nextFor(this.getLibraries(), (ea: LibraryVersion) => ea.getName(), "library");
    this.addNewLibraryImpl(null, {
      name: nextLibraryName,
      functionBody: LibraryVersion.defaultLibraryCode()
    });
  }

  cloneLibrary(libraryIdToClone: string): void {
    this.addNewLibraryImpl(libraryIdToClone);
  }

  selectRequiredApiConfig(required: RequiredApiConfig, callback?: () => void): void {
    this.setState({
      selectedApiConfigId: required.id
    }, callback);
  }

  toggleConfigureApiPanel(): void {
    this.props.onToggleActivePanel("configureApi", true);
  }

  onApiConfigClick(required: RequiredApiConfig): void {
    this.selectRequiredApiConfig(required, this.toggleConfigureApiPanel);
  }

  onAddApiConfigClick(): void {
    this.setState({
      selectedApiConfigId: null
    }, this.toggleConfigureApiPanel);
  }

  renderBehaviorSwitcher() {
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
              tests={this.getTests()}
              nodeModuleVersions={this.getNodeModuleVersions()}
              selectedId={this.getSelectedId()}
              groupId={this.getBehaviorGroup().id}
              onSelect={this.onSelect}
              addNewAction={this.addNewAction}
              addNewDataType={this.addNewDataType}
              addNewTest={this.addNewTest}
              addNewLibrary={this.addNewLibrary}
              isModified={this.editableIsModified}
              onUpdateNodeModules={this.updateNodeModules}
              requiredAWSConfigs={this.getRequiredAWSConfigs()}
              requiredOAuthApplications={this.getRequiredOAuthApiConfigs()}
              requiredSimpleTokenApis={this.getRequiredSimpleTokenApis()}
              onApiConfigClick={this.onApiConfigClick}
              onAddApiConfigClick={this.onAddApiConfigClick}
              getApiConfigName={this.getApiConfigName}
              updatingNodeModules={this.state.updatingNodeModules}
              runningTests={this.state.runningTests}
              testResults={this.state.testResults}
            />
          </Sticky>
        </Collapsible>
      </div>
    );
  }

  renderNameAndManagementActions(selected: Editable) {
    return (
      <div className="container container-wide bg-white">
        <div className="columns columns-elastic mobile-columns-float">
          <div className="column column-shrink">
            <FormInput
              className="form-input-borderless form-input-l type-l type-semibold width-15 mobile-width-full"
              ref={(el) => this.editableNameInput = el}
              value={this.getEditableName()}
              placeholder={selected.namePlaceholderText()}
              onChange={this.updateName}
            />
          </div>
          <div className="column column-expand align-r align-m mobile-align-l mobile-mtl">
            {this.isExistingEditable() ? (
              <div>
                <div className="mobile-display-inline-block mobile-mrs align-t">
                  <Button
                    className="button-s mbs"
                    onClick={this.cloneEditable}>
                    {selected.cloneActionText()}
                  </Button>
                </div>
                <div className="mobile-display-inline-block align-t">
                  <Button
                    className="button-s"
                    onClick={this.confirmDeleteEditable}>
                    {selected.deleteActionText()}
                  </Button>
                </div>
              </div>
            ) : (
              <div>
                <Button
                  className="button-s"
                  onClick={this.deleteEditable}
                >{selected.cancelNewText()}</Button>
              </div>
            )}
          </div>
        </div>
      </div>
    );
  }

  renderUserInputConfiguration(selected: BehaviorVersion) {
    if (selected.usesCode()) {
      return (
        <UserInputConfiguration
          onInputChange={this.updateBehaviorInputAtIndexWith}
          onInputMove={this.moveBehaviorInputAtIndex}
          onInputDelete={this.deleteInputAtIndex}
          onInputAdd={this.addNewInput}
          onInputNameFocus={this.onInputNameFocus}
          onInputNameBlur={this.onInputNameBlur}
          onConfigureType={this.onConfigureType}
          userInputs={this.getInputs()}
          paramTypes={this.getParamTypesForInput()}
          triggers={this.getBehaviorTriggers()}
          hasSharedAnswers={this.getOtherSavedInputsInGroup().length > 0}
          otherBehaviorsInGroup={this.otherBehaviorsInGroup()}
          onToggleSharedAnswer={this.toggleSharedAnswerInputSelector}
          savedAnswers={this.props.savedAnswers}
          onToggleSavedAnswer={this.toggleSavedAnswerEditor}
          onToggleInputHelp={this.toggleUserInputHelp}
          helpInputVisible={this.props.activePanelName === 'helpForUserInput'}
        />
      );
    } else {
      return null;
    }
  }

  renderNormalBehavior(selected: BehaviorVersion) {
    const template = selected.responseTemplate || ResponseTemplate.fromString("");
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

                {this.renderUserInputConfiguration(selected)}

                <hr className="man rule-subtle" />

                <div>
                  {this.renderCodeEditorForBehavior()}

                  <hr className="man rule-subtle" />
                </div>

                <ResponseTemplateConfiguration
                  availableHeight={this.getAvailableHeight()}
                  template={template}
                  onChangeTemplate={this.updateTemplate}
                  responseTypeId={selected.config.responseTypeId}
                  possibleResponseTypes={this.props.possibleResponseTypes}
                  onSelectResponseType={this.onSelectResponseType}
                  onCursorChange={this.ensureCursorVisible}
                  onToggleHelp={this.toggleResponseTemplateHelp}
                  helpVisible={this.props.activePanelName === 'helpForResponseTemplate'}
                  sectionNumber={"4"}
                />

      </div>
    );
  }

  renderDataTypeBehavior(selected: BehaviorVersion) {
    return (
      <div>
        <div className="bg-white pbl" />
        <hr className="mtn mbn rule-subtle" />

        <DataTypeEditor
          ref={(el) => this.dataTypeEditor = el}
          group={this.getBehaviorGroup()}
          behaviorVersion={selected}
          paramTypes={this.getParamTypes()}
          builtinParamTypes={this.props.builtinParamTypes}
          inputs={this.getInputs()}
          onChange={this.updateDataType}
          onConfigureType={this.onConfigureType}
          isModified={this.editableIsModified}

          activePanelName={this.props.activePanelName}
          activeDropdownName={this.getActiveDropdown()}
          onToggleActiveDropdown={this.toggleActiveDropdown}
          onToggleActivePanel={this.toggleActivePanel}
          animationIsDisabled={this.animationIsDisabled()}

          userInputConfiguration={this.renderUserInputConfiguration(selected)}
          codeConfiguration={this.renderCodeEditorForBehavior()}
        />
      </div>
    );
  }

  renderCodeEditorForBehavior() {
    const selected = this.getSelectedBehavior();
    if (selected && selected.usesCode()) {
      return this.renderCodeEditor({
        sectionNumber: "3",
        codeHelpPanelName: 'helpForBehaviorCode',
        isMemoizationEnabled: true
      });
    } else {
      return null;
    }
  }

  renderForSelected(selected: Editable) {
    if (selected.isBehaviorVersion()) {
      if (selected.isDataType()) {
        return this.renderDataTypeBehavior(selected);
      } else if (selected.isTest()) {
        return this.renderTest();
      } else {
        return this.renderNormalBehavior(selected);
      }
    } else if (selected.isLibraryVersion()) {
      return this.renderLibrary();
    } else {
      throw new Error("Unable to render unknown editable component!");
    }
  }

  renderLibrary() {
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
          functionExecutesImmediately: true,
          isMemoizationEnabled: false
        })}
      </div>
    );
  }

  renderTest() {
    return (
      <div className="pbxxxl">

        <div className="columns container container-wide bg-white">
          <div className="column column-full mobile-column-full">
            <FormInput
              className="form-input-borderless form-input-m mbneg1"
              placeholder="Test description (optional)"
              onChange={this.updateDescription}
              value={this.getEditableDescription()}
            />
          </div>
        </div>

        <hr className="man rule-subtle" />

        {this.renderCodeEditor({
          sectionNumber: "1",
          codeHelpPanelName: 'helpForTestCode',
          isMemoizationEnabled: false
        })}

        <hr className="man rule-subtle" />

        <TestOutput
          sectionNumber={"2"}
          testResult={this.getSelectedTestResult()}
        />

      </div>
    );
  }

  renderEditor() {
    const selected = this.getSelected();
    if (selected) {
      return (
        <div>
          <div className="container container-wide ptl bg-white">
            <h5 className="type-blue-faded mvn">{selected.getEditorTitle()}</h5>
          </div>

          {this.renderNameAndManagementActions(selected)}
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
          iconPickerVisible={this.getActiveDropdown() === "behaviorGroupEditorIconPicker"}
          onToggleIconPicker={this.toggleGroupEditorIconPicker}
        />
      );
    }
  }

  renderEditorPage() {
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
        </form>

        {this.renderHiddenForms()}

      </div>
    );
  }

  renderVersionBrowser() {
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
        isLinkedToGithub={this.props.isLinkedToGithub}
        linkedGithubRepo={this.props.linkedGithubRepo}
        onLinkGithubRepo={this.props.onLinkGithubRepo}
        onChangedGithubRepo={this.toggleChangeGithubRepo}
        onUpdateFromGithub={this.props.onUpdateFromGithub}
        onSaveChanges={this.onSaveClick}
        isModifyingGithubRepo={this.state.isModifyingGithubRepo}
      />
    );
  }

  renderNavItems() {
    const versionBrowserOpen = this.props.activePanelName === 'versionBrowser';
    const indexUrl = jsRoutes.controllers.ApplicationController.index(this.props.isAdmin ? this.props.group.teamId : null).url;
    const items: Array<NavItemContent> = [{
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
  }

  renderDeployStatus() {
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
  }

  renderGithubRepoActions() {
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
  }

  renderNavActions() {
    if (this.state.versionBrowserOpen) {
      this.props.onRenderNavActions(this.renderGithubRepoActions());
    } else {
      this.props.onRenderNavActions(this.renderDeployStatus());
    }
  }

  render() {
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
}

export default BehaviorEditor;
