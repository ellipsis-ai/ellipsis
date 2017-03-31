define(function(require) {
  var React = require('react'),
    APISelectorMenu = require('./api_selector_menu'),
    AWSConfig = require('./aws_config'),
    CodeEditor = require('./code_editor'),
    CodeFooter = require('./code_footer'),
    CodeHeader = require('./code_header'),
    Collapsible = require('../shared_ui/collapsible'),
    DropdownMenu = require('../shared_ui/dropdown_menu'),
    Input = require('../models/input'),
    oauth2ApplicationShape = require('./oauth2_application_shape');


  var apiShape = React.PropTypes.shape({
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
      onToggleAWSHelp: React.PropTypes.func.isRequired,
      awsConfig: React.PropTypes.object,
      onAWSAddNewEnvVariable: React.PropTypes.func.isRequired,
      onAWSConfigChange: React.PropTypes.func.isRequired,

      allOAuth2Applications: React.PropTypes.arrayOf(oauth2ApplicationShape).isRequired,
      requiredOAuth2ApiConfigs: React.PropTypes.arrayOf(oauth2ApplicationShape).isRequired,
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
      canDeleteFunctioBody: React.PropTypes.bool.isRequired,
      onDeleteFunctionBody: React.PropTypes.func.isRequired,

      envVariableNames: React.PropTypes.arrayOf(React.PropTypes.string).isRequired
    },

    render: function() {
      return (
        <div>
          <div className="border-top border-left border-right border-light ptm">
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

              <Collapsible revealWhen={!!this.getAWSConfig()} animationDisabled={this.animationIsDisabled()}>
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
                    helpVisible={this.props.activePanelName === 'helpForAWS'}
                  />
                </div>
              </Collapsible>
            </div>

            <CodeHeader
              ref="codeHeader"
              userInputs={this.getInputs()}
              systemParams={this.getSystemParams()}
            />
          </div>

          <div className="position-relative">
            <CodeEditor
              ref="codeEditor"
              value={this.getBehaviorFunctionBody()}
              onChange={this.updateCode}
              onCursorChange={this.ensureCursorVisible}
              firstLineNumber={this.getFirstLineNumberForCode()}
              lineWrapping={this.state.codeEditorUseLineWrapping}
              functionParams={this.getCodeFunctionParams()}
              apiAccessTokens={this.getApiApplications()}
              envVariableNames={this.getEnvVariableNames()}
              hasAwsConfig={!!this.getAWSConfig()}
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
    }
  });
});
