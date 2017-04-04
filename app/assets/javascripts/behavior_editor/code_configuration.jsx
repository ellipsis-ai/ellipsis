define(function(require) {
  const React = require('react'),
    AWSConfig = require('./aws_config'),
    CodeEditor = require('./code_editor'),
    Collapsible = require('../shared_ui/collapsible'),
    DropdownMenu = require('../shared_ui/dropdown_menu'),
    HelpButton = require('../help/help_button'),
    Input = require('../models/input'),
    Notifications = require('../notifications/notifications'),
    NotificationData = require('../models/notification_data'),
    SectionHeading = require('./section_heading'),
    SVGSettingsIcon = require('../svg/settings'),
    debounce = require('javascript-debounce');

  return React.createClass({
    displayName: 'CodeConfiguration',
    propTypes: {
      sectionNumber: React.PropTypes.string.isRequired,
      sectionHeading: React.PropTypes.string.isRequired,
      codeEditorHelp: React.PropTypes.node.isRequired,

      activePanelName: React.PropTypes.string.isRequired,
      activeDropdownName: React.PropTypes.string.isRequired,
      onToggleActiveDropdown: React.PropTypes.func.isRequired,
      onToggleActivePanel: React.PropTypes.func.isRequired,
      animationIsDisabled: React.PropTypes.bool.isRequired,

      onToggleAWSConfig: React.PropTypes.func.isRequired,
      awsConfig: React.PropTypes.object,
      onAWSAddNewEnvVariable: React.PropTypes.func.isRequired,
      onAWSConfigChange: React.PropTypes.func.isRequired,

      apiSelector: React.PropTypes.node.isRequired,

      inputs: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Input)).isRequired,
      systemParams: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,

      functionBody: React.PropTypes.string.isRequired,
      onChangeFunctionBody: React.PropTypes.func.isRequired,
      onCursorChange: React.PropTypes.func.isRequired,
      useLineWrapping: React.PropTypes.bool.isRequired,
      onToggleCodeEditorLineWrapping: React.PropTypes.func.isRequired,
      canDeleteFunctionBody: React.PropTypes.bool.isRequired,
      onDeleteFunctionBody: React.PropTypes.func.isRequired,

      envVariableNames: React.PropTypes.arrayOf(React.PropTypes.string).isRequired
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

    toggleAWSHelp: function() {
      this.props.onToggleActivePanel('helpForAWS');
    },

    toggleBoilerplateHelp: function() {
      this.props.onToggleActivePanel('helpForBoilerplateParameters');
    },

    toggleEditorSettingsMenu: function() {
      this.props.onToggleActiveDropdown('codeEditorSettings');
    },

    hasAwsConfig: function() {
      return !!this.props.awsConfig;
    },

    getAWSConfigProperty: function(property) {
      const config = this.props.awsConfig;
      if (config) {
        return config[property];
      } else {
        return "";
      }
    },

    getCodeFunctionParams: function() {
      const userParams = this.props.inputs.map(ea => ea.name);
      return userParams.concat(this.props.systemParams);
    },

    getApiApplicationsFrom: function(props) {
      return props.requiredOAuth2ApiConfigs
        .filter((config) => !!config.application)
        .map((config) => config.application);
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

    hasUsedOAuth2Application: function(code, keyName) {
      var pattern = new RegExp(`\\bellipsis\\.accessTokens\\.${keyName}\\b`);
      return pattern.test(code);
    },

    hasUsedAWSObject: function(code) {
      return /\bellipsis\.AWS\b/.test(code);
    },

    buildNotifications: function() {
      var props = this.props;
      var oAuth2Notifications = [];
      var awsNotifications = [];
      this.getApiApplicationsFrom(props)
        .filter((ea) => ea && !this.hasUsedOAuth2Application(props.functionBody, ea.keyName))
        .forEach((ea) => {
          oAuth2Notifications.push(new NotificationData({
            kind: "oauth2_application_unused",
            name: ea.displayName,
            code: `ellipsis.accessTokens.${ea.keyName}`
          }));
        });
      if (props.awsConfig && !this.hasUsedAWSObject(props.functionBody)) {
        awsNotifications.push(new NotificationData({
          kind: "aws_unused",
          code: "ellipsis.AWS"
        }));
      }
      return oAuth2Notifications.concat(awsNotifications);
    },

    getCodeAutocompletions: function() {
      var apiTokens = this.getApiApplicationsFrom(this.props).map((application) => `ellipsis.accessTokens.${application.keyName}`);

      var envVars = this.props.envVariableNames.map(function(name) {
        return `ellipsis.env.${name}`;
      });

      var aws = this.hasAwsConfig() ? ['ellipsis.AWS'] : [];

      return this.getCodeFunctionParams().concat(apiTokens, aws, envVars);
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
                  <span className="mrm">{this.props.sectionHeading}</span>
                  <span className="display-inline-block">
                    <HelpButton onClick={this.toggleBoilerplateHelp} toggled={this.props.activePanelName === 'helpForBoilerplateParameters'} />
                  </span>
                </SectionHeading>
              </div>
              <div className="column column-shrink ptxs mobile-mbm">{this.props.apiSelector}</div>
            </div>

            {this.props.codeEditorHelp}
          </div>

          <div>
            <Collapsible revealWhen={this.hasAwsConfig()} animationDisabled={this.props.animationIsDisabled} className="debugger">
              <div className="type-s plxxxl prs mbm">
                <AWSConfig
                  envVariableNames={this.props.envVariableNames}
                  accessKeyName={this.getAWSConfigProperty('accessKeyName')}
                  secretKeyName={this.getAWSConfigProperty('secretKeyName')}
                  regionName={this.getAWSConfigProperty('regionName')}
                  onAddNew={this.props.onAWSAddNewEnvVariable}
                  onChange={this.props.onAWSConfigChange}
                  onRemoveAWSConfig={this.props.onToggleAWSConfig}
                  onToggleHelp={this.toggleAWSHelp}
                  helpVisible={this.props.activePanelName === 'helpForAWS'}
                />
              </div>
            </Collapsible>

            <div className="pbxs">
              <div className="columns columns-elastic">
                <div className="column column-shrink plxxxl prn align-r position-relative">
                  <code className="type-disabled type-s position-absolute position-top-right prxs">1</code>
                </div>
                <div className="column column-expand plxs">
                  <code className="type-s">
                    <span className="type-s type-weak">{"function ("}</span>
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
                className="border-left border-right border-pink-light"
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
                    <code className="type-weak type-s">{"\u007D"}</code>
                  </div>
                  <div className="column column-shrink prs align-r">
                    {this.props.canDeleteFunctionBody ? (
                      <button type="button" className="button-s" onClick={this.props.onDeleteFunctionBody}>Remove code</button>
                    ) : null}
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
