import * as React from 'react';
import BehaviorConfig from '../models/behavior_config';
import CodeEditor from './code_editor';
import DropdownMenu from '../shared_ui/dropdown_menu';
import HelpButton from '../help/help_button';
import Input from '../models/input';
import Notifications from '../notifications/notifications';
import {RequiredAWSConfig} from '../models/aws';
import {RequiredOAuth2Application} from '../models/oauth2';
import SectionHeading from '../shared_ui/section_heading';
import SVGSettingsIcon from '../svg/settings';
import SVGWarning from '../svg/warning';
import ToggleGroup from '../form/toggle_group';
import debounce from 'javascript-debounce';
import OAuth2ApplicationUnusedNotificationData from "../models/notifications/oauth2_application_unused";
import AWSUnusedNotificationData from "../models/notifications/aws_unused_notification_data";

const CodeConfiguration = React.createClass({
    propTypes: {
      sectionNumber: React.PropTypes.string.isRequired,
      codeHelpPanelName: React.PropTypes.string.isRequired,

      activePanelName: React.PropTypes.string.isRequired,
      activeDropdownName: React.PropTypes.string.isRequired,
      onToggleActiveDropdown: React.PropTypes.func.isRequired,
      onToggleActivePanel: React.PropTypes.func.isRequired,
      animationIsDisabled: React.PropTypes.bool.isRequired,

      behaviorConfig: React.PropTypes.instanceOf(BehaviorConfig),

      inputs: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Input)).isRequired,
      systemParams: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,

      requiredAWSConfigs: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredAWSConfig)).isRequired,

      apiApplications: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredOAuth2Application)).isRequired,

      functionBody: React.PropTypes.string.isRequired,
      onChangeFunctionBody: React.PropTypes.func.isRequired,
      onCursorChange: React.PropTypes.func.isRequired,
      useLineWrapping: React.PropTypes.bool.isRequired,
      onToggleCodeEditorLineWrapping: React.PropTypes.func.isRequired,

      onChangeCanBeMemoized: React.PropTypes.func.isRequired,

      envVariableNames: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
      functionExecutesImmediately: React.PropTypes.bool
    },

    getInitialState: function() {
      return {
        notifications: this.buildNotifications()
      };
    },

    componentDidMount: function() {
      this.updateNotifications = debounce(this.updateNotifications, 250);
    },

    componentWillReceiveProps: function() {
      this.updateNotifications();
    },

    updateNotifications: function() {
      if (this.refs.notifications) {
        this.setState({
          notifications: this.buildNotifications()
        });
      }
    },

    toggleBehaviorCodeHelp: function() {
      this.props.onToggleActivePanel(this.props.codeHelpPanelName);
    },

    toggleEditorSettingsMenu: function() {
      this.props.onToggleActiveDropdown('codeEditorSettings');
    },

    getCodeFunctionParams: function() {
      const userParams = this.props.inputs.map(ea => ea.name);
      return userParams.concat(this.props.systemParams);
    },

    getCodeEditorDropdownLabel: function() {
      return (<SVGSettingsIcon label="Editor settings" />);
    },

    getFirstLineNumberForCode: function() {
      return 2;
    },

    getLastLineNumberForCode: function() {
      const numLines = this.props.functionBody.split('\n').length;
      return this.getFirstLineNumberForCode() + numLines;
    },

    hasUsedOAuth2Application: function(code, nameInCode) {
      var pattern = new RegExp(`\\bellipsis\\.accessTokens\\.${nameInCode}\\b`);
      return pattern.test(code);
    },

    hasUsedAWSConfig: function(code, nameInCode) {
      var pattern = new RegExp(`\\bellipsis\\.aws\\.${nameInCode}\\b`);
      return pattern.test(code);
    },

    buildNotifications: function() {
      var oAuth2Notifications = [];
      var awsNotifications = [];
      this.props.apiApplications
        .filter((ea) => ea && !this.hasUsedOAuth2Application(this.props.functionBody, ea.nameInCode))
        .forEach((ea) => {
          oAuth2Notifications.push(new OAuth2ApplicationUnusedNotificationData({
            name: ea.config.displayName,
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
      return oAuth2Notifications.concat(awsNotifications);
    },

    getCodeAutocompletions: function() {
      var apiTokens = this.props.apiApplications.map(ea => `ellipsis.accessTokens.${ea.nameInCode}`);
      var envVars = this.props.envVariableNames.map(function(name) {
        return `ellipsis.env.${name}`;
      });
      var awsTokens = this.props.requiredAWSConfigs.map(ea => `ellipsis.aws.${ea.nameInCode}`);

      return this.getCodeFunctionParams().concat(apiTokens, awsTokens, envVars);
    },

    refresh: function() {
      this.refs.codeEditor.refresh();
    },

    unsetCanBeMemoized: function() {
      this.props.onChangeCanBeMemoized(false);
    },

    setCanBeMemoized: function() {
      this.props.onChangeCanBeMemoized(true);
    },

    canBeMemoized: function() {
      return this.props.behaviorConfig.canBeMemoized;
    },

    renderCacheWarning: function() {
      if (this.canBeMemoized()) {
        return (
          <span>
            <span className="display-inline-block align-b type-yellow plm prxs" style={{ width: 22, height: 24 }}>
              <SVGWarning />
            </span>
            <span className="display-inline-block align-b type-s">Only cache results if the function will always return the same result given the same parameters.</span>
          </span>
        );
      } else {
        return null;
      }
    },

    render: function() {
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

          <div>

            <div className="pbxs">
              <div className="border-top border-left border-right border-light pas">
                <ToggleGroup className="form-toggle-group-s align-m">
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
              <div className="columns columns-elastic">
                <div className="column column-shrink plxxxl prn align-r position-relative">
                  <code className="type-disabled type-s position-absolute position-top-right prxs">1</code>
                </div>
                <div className="column column-expand pll">
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

            <div style={{ marginLeft: "60px" }}>
              <Notifications
                ref="notifications"
                notifications={this.state.notifications}
                inline={true}
              />
            </div>

          </div>

          <div className="position-relative">
            <CodeEditor
              ref="codeEditor"
              value={this.props.functionBody}
              onChange={this.props.onChangeFunctionBody}
              onCursorChange={this.props.onCursorChange}
              firstLineNumber={this.getFirstLineNumberForCode()}
              lineWrapping={this.props.useLineWrapping}
              functionParams={this.getCodeFunctionParams()}
              autocompletions={this.getCodeAutocompletions()}
            />
            <div className="position-absolute position-top-right position-z-popup-trigger">
              <DropdownMenu
                openWhen={this.props.activeDropdownName === 'codeEditorSettings'}
                label={this.getCodeEditorDropdownLabel()}
                labelClassName="button-dropdown-trigger-symbol"
                menuClassName="popup-dropdown-menu-right"
                toggle={this.toggleEditorSettingsMenu}
              >
                <DropdownMenu.Item
                  onClick={this.props.onToggleCodeEditorLineWrapping}
                  checkedWhen={this.props.useLineWrapping}
                  label="Enable line wrap"
                />
              </DropdownMenu>
            </div>
          </div>

          <div className="pts mbxxl">
            <div className="columns columns-elastic">
              <div className="column column-shrink plxxxl prn align-r position-relative">
                <code className="type-disabled type-s position-absolute position-top-right prxs">{this.getLastLineNumberForCode()}</code>
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
});

export default CodeConfiguration;
