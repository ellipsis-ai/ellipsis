define(function(require) {
  const React = require('react'),
    APISelectorMenu = require('./api_selector_menu'),
    AWSConfig = require('./aws_config'),
    CodeEditor = require('./code_editor'),
    Collapsible = require('../shared_ui/collapsible'),
    DropdownMenu = require('../shared_ui/dropdown_menu'),
    Input = require('../models/input'),
    SVGSettingsIcon = require('../svg/settings'),
    oauth2ApplicationShape = require('./oauth2_application_shape');

  const apiShape = React.PropTypes.shape({
    apiId: React.PropTypes.string.isRequired,
    name: React.PropTypes.string.isRequired
  });

  return React.createClass({
    displayName: 'CodeConfiguration',
    propTypes: {
      activePanelName: React.PropTypes.string.isRequired,
      activeDropdownName: React.PropTypes.string.isRequired,
      onToggleActiveDropdown: React.PropTypes.func.isRequired,
      onToggleActivePanel: React.PropTypes.func.isRequired,
      animationIsDisabled: React.PropTypes.bool.isRequired,

      onToggleAWSConfig: React.PropTypes.func.isRequired,
      awsConfig: React.PropTypes.object,
      onAWSAddNewEnvVariable: React.PropTypes.func.isRequired,
      onAWSConfigChange: React.PropTypes.func.isRequired,

      allOAuth2Applications: React.PropTypes.arrayOf(oauth2ApplicationShape).isRequired,
      requiredOAuth2ApiConfigs: React.PropTypes.arrayOf(React.PropTypes.shape({
        apiId: React.PropTypes.string.isRequired
      })).isRequired,
      allSimpleTokenApis: React.PropTypes.arrayOf(apiShape).isRequired,
      requiredSimpleTokenApis: React.PropTypes.arrayOf(apiShape).isRequired,
      onAddOAuth2Application: React.PropTypes.func.isRequired,
      onRemoveOAuth2Application: React.PropTypes.func.isRequired,
      onAddSimpleTokenApi: React.PropTypes.func.isRequired,
      onRemoveSimpleTokenApi: React.PropTypes.func.isRequired,
      onNewOAuth2Application: React.PropTypes.func.isRequired,
      getOAuth2ApiWithId: React.PropTypes.func.isRequired,

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

    toggleAPISelectorMenu: function() {
      this.props.onToggleActiveDropdown('apiSelectorDropdown');
    },

    toggleAWSHelp: function() {
      this.props.onToggleActivePanel('helpForAWS');
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

    getApiApplications: function() {
      return this.props.requiredOAuth2ApiConfigs
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

    refresh: function() {
      this.refs.codeEditor.refresh();
    },

    render: function() {
      return (
        <div>
          <div className="border-top border-left border-right border-light ptm">
            <div className="type-s">
              <div className="plxxxl prs mbm">
                <APISelectorMenu
                  openWhen={this.props.activeDropdownName === 'apiSelectorDropdown'}
                  onAWSClick={this.props.onToggleAWSConfig}
                  awsCheckedWhen={this.hasAwsConfig()}
                  toggle={this.toggleAPISelectorMenu}
                  allOAuth2Applications={this.props.allOAuth2Applications}
                  requiredOAuth2ApiConfigs={this.props.requiredOAuth2ApiConfigs}
                  allSimpleTokenApis={this.props.allSimpleTokenApis}
                  requiredSimpleTokenApis={this.props.requiredSimpleTokenApis}
                  onAddOAuth2Application={this.props.onAddOAuth2Application}
                  onRemoveOAuth2Application={this.props.onRemoveOAuth2Application}
                  onAddSimpleTokenApi={this.props.onAddSimpleTokenApi}
                  onRemoveSimpleTokenApi={this.props.onRemoveSimpleTokenApi}
                  onNewOAuth2Application={this.props.onNewOAuth2Application}
                  getOAuth2ApiWithId={this.props.getOAuth2ApiWithId}
                />
              </div>

              <Collapsible revealWhen={this.hasAwsConfig()} animationDisabled={this.props.animationIsDisabled}>
                <div className="plxxxl prs pbs mbs border-bottom border-light">
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
            </div>

            <div className="pbxs">
              <div className="columns columns-elastic">
                <div className="column column-shrink plxxxl prn align-r position-relative">
                  <code className="type-disabled type-s position-absolute position-top-right">1</code>
                </div>
                <div className="column column-expand pls">
                  <code className="type-s">
                    <span className="type-s type-weak">{"function ("}</span>
                    {this.props.inputs.map((input, inputIndex) => (
                      <span key={`input${input.inputId || inputIndex}`}>{input.name}<span className="type-weak">, </span></span>
                    ))}
                    <span className="type-weak">{this.props.systemParams.join(", ")}</span>
                    <span className="type-weak">{") \u007B"}</span>
                  </code>
                </div>
              </div>
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
              apiAccessTokens={this.getApiApplications()}
              envVariableNames={this.props.envVariableNames}
              hasAwsConfig={this.hasAwsConfig()}
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

          <div className="border-left border-right border-bottom border-light pvs mbxxl">
            <div className="columns columns-elastic">
              <div className="column column-shrink plxxxl prn align-r position-relative">
                <code className="type-disabled type-s position-absolute position-top-right">{this.getLastLineNumberForCode()}</code>
              </div>
              <div className="column column-expand pls">
                <div className="columns columns-elastic">
                  <div className="column column-expand">
                    <code className="type-weak type-s">{"}"}</code>
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
