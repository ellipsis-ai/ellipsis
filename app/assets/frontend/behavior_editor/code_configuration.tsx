import * as React from 'react';
import BehaviorConfig from '../models/behavior_config';
import CodeEditor, {EditorCursorPosition} from './code_editor';
import DropdownMenu, {DropdownMenuItem} from '../shared_ui/dropdown_menu';
import HelpButton from '../help/help_button';
import Input from '../models/input';
import Notifications from '../notifications/notifications';
import {RequiredAWSConfig} from '../models/aws';
import {RequiredOAuthApplication} from '../models/oauth';
import SectionHeading from '../shared_ui/section_heading';
import SVGSettingsIcon from '../svg/settings';
import SVGWarning from '../svg/warning';
import ToggleGroup from '../form/toggle_group';
import * as debounce from 'javascript-debounce';
import OAuthApplicationUnusedNotificationData from "../models/notifications/oauth_application_unused";
import AWSUnusedNotificationData from "../models/notifications/aws_unused_notification_data";
import NotificationData from "../models/notifications/notification_data";
import autobind from "../lib/autobind";
import EllipsisObjectDefinitions from "../code_editor/definitions/ellipsis";
import NodeModuleVersion from "../models/node_module_version";
import LibraryVersion from "../models/library_version";

interface Props {
  availableHeight: number

  sectionNumber: string,
  codeHelpPanelName: string,

  activePanelName: string,
  activeDropdownName: string,
  onToggleActiveDropdown: (s: string) => void,
  onToggleActivePanel: (s: string) => void,
  animationIsDisabled: boolean,

  behaviorConfig: Option<BehaviorConfig>,

  inputs: Array<Input>,
  systemParams: Array<string>,

  requiredAWSConfigs: Array<RequiredAWSConfig>,

  oauthApiApplications: Array<RequiredOAuthApplication>,

  libraries: Array<LibraryVersion>,

  nodeModules: Array<NodeModuleVersion>,

  functionBody: string,
  firstLineNumber: number,
  lastLineNumber: number,
  onChangeFunctionBody: (s: string) => void,
  onCursorChange: (newPosition: EditorCursorPosition) => void,
  useLineWrapping: boolean,
  onToggleCodeEditorLineWrapping: () => void,

  onChangeCanBeMemoized: (canBeMemoized: boolean) => void,
  isMemoizationEnabled: boolean,

  envVariableNames: Array<string>,
  functionExecutesImmediately?: Option<boolean>,

  paramTypeIds: Array<string>,
  isDataType: boolean
}

interface State {
  notifications: Array<NotificationData>
}

class CodeConfiguration extends React.Component<Props, State> {
    updateNotifications: () => void;
    notificationComponent: Option<Notifications>;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.state = {
        notifications: this.buildNotifications()
      };
      this.updateNotifications = debounce(this.updateNotificationsNow, 250);
    }

    componentWillReceiveProps(): void {
      this.updateNotifications();
    }

    updateNotificationsNow(): void {
      if (this.notificationComponent) {
        this.setState({
          notifications: this.buildNotifications()
        });
      }
    }

    toggleBehaviorCodeHelp(): void {
      this.props.onToggleActivePanel(this.props.codeHelpPanelName);
    }

    toggleEditorSettingsMenu(): void {
      this.props.onToggleActiveDropdown('codeEditorSettings');
    }

    getCodeEditorDropdownLabel() {
      return (<SVGSettingsIcon label="Editor settings" />);
    }

    hasUsedOAuthApplication(code: string, nameInCode: string): boolean {
      var pattern = new RegExp(`\\bellipsis\\.accessTokens\\.${nameInCode}\\b`);
      return pattern.test(code);
    }

    hasUsedAWSConfig(code: string, nameInCode: string): boolean {
      var pattern = new RegExp(`\\bellipsis\\.aws\\.${nameInCode}\\b`);
      return pattern.test(code);
    }

    buildNotifications(): Array<NotificationData> {
      var oAuthNotifications: Array<NotificationData> = [];
      var awsNotifications: Array<NotificationData> = [];
      this.props.oauthApiApplications
        .filter((ea) => ea && !this.hasUsedOAuthApplication(this.props.functionBody, ea.nameInCode))
        .forEach((ea) => {
          oAuthNotifications.push(new OAuthApplicationUnusedNotificationData({
            name: ea.config ? ea.config.displayName : "Unknown",
            code: `ellipsis.accessTokens.${ea.nameInCode}`
          }));
        });
      this.props.requiredAWSConfigs
        .filter(ea => !this.hasUsedAWSConfig(this.props.functionBody, ea.nameInCode))
        .forEach(ea => {
          awsNotifications.push(new AWSUnusedNotificationData({
            code: `ellipsis.aws.${ea.nameInCode}`
          }));
        });
      return oAuthNotifications.concat(awsNotifications);
    }

    emptyModuleFor(name: string): string {
      return `declare module "${name}";`;
    }

    getCodeDefinitions(): string {
      const ellipsisObjectDefinitions = this.props.systemParams.includes("ellipsis") ?
        EllipsisObjectDefinitions.buildFor({
        requiredAWSConfigs: this.props.requiredAWSConfigs,
        oauthApiApplications: this.props.oauthApiApplications,
        envVariableNames: this.props.envVariableNames,
        paramTypeIds: this.props.paramTypeIds,
        isDataType: this.props.isDataType
      }) : "";
      return `
${ellipsisObjectDefinitions}

${this.props.inputs.map(ea => `declare var ${ea.name}: ${ea.paramType ? ea.paramType.typescriptType : "any"};`).join("\n")}

${this.props.libraries.map((ea) => ea.name ? this.emptyModuleFor(ea.name) : "").join("\n")}

${this.props.nodeModules.map((ea) => this.emptyModuleFor(ea.from)).join("\n")}
`
    }

    unsetCanBeMemoized(): void {
      this.props.onChangeCanBeMemoized(false);
    }

    setCanBeMemoized(): void {
      this.props.onChangeCanBeMemoized(true);
    }

    canBeMemoized(): boolean {
      return Boolean(this.props.behaviorConfig && this.props.behaviorConfig.canBeMemoized);
    }

    renderCacheWarning() {
      if (this.canBeMemoized()) {
        return (
          <div className="display-inline-block align-b type-preserve-spaces">
            <span style={{ height: 24 }} className="display-inline-block type-yellow mrs align-b type-s"><SVGWarning /></span>
            <span className="type-s">Only cache results if the function will always return the same result given the same parameters.</span>
          </div>
        );
      } else {
        return null;
      }
    }

    renderToggleCanBeMemoized() {
      if (this.props.isMemoizationEnabled) {
        return (
          <div className="plxl pbm">
            <ToggleGroup className="form-toggle-group-s align-m mrl">
              <ToggleGroup.Item
                title="This code will run every time"
                label="Always run this function"
                activeWhen={!this.canBeMemoized()}
                onClick={this.unsetCanBeMemoized}
              />
              <ToggleGroup.Item
                title="The result of this code will be cached for a given set of parameters"
                label="Cache results"
                activeWhen={this.canBeMemoized()}
                onClick={this.setCanBeMemoized}
              />
            </ToggleGroup>
            {this.renderCacheWarning()}
          </div>
        );
      } else {
        return null;
      }
    }

    render() {
      return (
        <div>

          <div className="container container-wide">
            <div className="ptxl columns columns-elastic mobile-columns-float">
              <div className="column column-expand">
                <SectionHeading number={this.props.sectionNumber}>
                  <span className="mrm">Code</span>
                  <span className="display-inline-block">
                    <HelpButton onClick={this.toggleBehaviorCodeHelp} toggled={this.props.activePanelName === 'helpForBehaviorCode'} />
                  </span>
                </SectionHeading>
              </div>
            </div>
          </div>

          <div className="position-relative">

            <div className="pbxs">
              {this.renderToggleCanBeMemoized()}
              <div className="columns columns-elastic">
                <div className="column column-shrink plxxxl prn align-r position-relative">
                  <code className="type-disabled type-s position-absolute position-top-right prxs">1</code>
                </div>
                <div className="column column-expand pls">
                  <code className="type-s">
                    <span className="type-s type-weak">{
                      this.props.functionExecutesImmediately ?
                        "module.exports = (function(" :
                        "module.exports = function("
                    }</span>
                    {this.props.inputs.map((input, inputIndex) => (
                      <span key={`param${inputIndex}`}>{input.name}<span className="type-weak">, </span></span>
                    ))}
                    <span className="type-weak">{this.props.systemParams.join(", ")}</span>
                    <span className="type-weak">{") \u007B"}</span>
                  </code>
                </div>
              </div>
            </div>

            <div style={{ marginLeft: "54px" }}>
              <Notifications
                ref={(el) => this.notificationComponent = el}
                notifications={this.state.notifications}
                inline={true}
              />
            </div>

            <div className="position-absolute position-bottom-right position-z-popup-trigger">
              <DropdownMenu
                openWhen={this.props.activeDropdownName === 'codeEditorSettings'}
                label={this.getCodeEditorDropdownLabel()}
                labelClassName="button-dropdown-trigger-symbol"
                menuClassName="popup-dropdown-menu-right"
                toggle={this.toggleEditorSettingsMenu}
              >
                <DropdownMenuItem
                  onClick={this.props.onToggleCodeEditorLineWrapping}
                  checkedWhen={this.props.useLineWrapping}
                  label="Enable line wrap"
                />
              </DropdownMenu>
            </div>
          </div>

          <div>
            <CodeEditor
              availableHeight={this.props.availableHeight}
              value={this.props.functionBody}
              onChange={this.props.onChangeFunctionBody}
              onCursorChange={this.props.onCursorChange}
              firstLineNumber={this.props.firstLineNumber}
              lineWrapping={this.props.useLineWrapping}
              definitions={this.getCodeDefinitions()}
              language={"javascript"}
            />
          </div>

          <div className="pts mbxxl">
            <div className="columns columns-elastic">
              <div className="column column-shrink plxxxl prn align-r position-relative">
                <code className="type-disabled type-s position-absolute position-top-right prxs">{this.props.lastLineNumber}</code>
              </div>
              <div className="column column-expand pll">
                <div className="columns columns-elastic">
                  <div className="column column-expand">
                    <code className="type-weak type-s">{
                      this.props.functionExecutesImmediately ? "\u007D)();" : "\u007D;"
                    }</code>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    }
}

export default CodeConfiguration;
