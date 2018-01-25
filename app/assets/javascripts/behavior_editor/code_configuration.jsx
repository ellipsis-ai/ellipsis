define(function(require) {
  const React = require('react'),
    BehaviorConfig = require('../models/behavior_config'),
    CodeEditor = require('./code_editor'),
    DropdownMenu = require('../shared_ui/dropdown_menu'),
    HelpButton = require('../help/help_button'),
    Input = require('../models/input'),
    Notifications = require('../notifications/notifications'),
    NotificationData = require('../models/notification_data'),
    RequiredAWSConfig = require('../models/aws').RequiredAWSConfig,
    RequiredOAuth2Application = require('../models/oauth2').RequiredOAuth2Application,
    SectionHeading = require('../shared_ui/section_heading'),
    SVGSettingsIcon = require('../svg/settings'),
    debounce = require('javascript-debounce');

  return React.createClass({
    displayName: 'CodeConfiguration',
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
          oAuth2Notifications.push(new NotificationData({
            kind: "oauth2_application_unused",
            name: ea.config.displayName,
            code: `ellipsis.accessTokens.${ea.nameInCode}`
          }));
        });
      this.props.requiredAWSConfigs
        .filter(ea => !this.hasUsedAWSConfig(this.props.functionBody, ea.nameInCode))
        .forEach(ea => {
          awsNotifications.push(new NotificationData({
            kind: "aws_unused",
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
              <div className="columns columns-elastic">
                <div className="column column-shrink plxxxl prn align-r position-relative">
                  <code className="type-disabled type-s position-absolute position-top-right prxs">1</code>
                </div>
                <div className="column column-expand plxs">
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

            <div style={{ marginLeft: "49px" }}>
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
              <div className="column column-expand plxs">
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
});
