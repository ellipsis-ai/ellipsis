define(function(require) {
  var React = require('react'),
    CodeConfiguration = require('./code_configuration'),
    Collapsible = require('../shared_ui/collapsible'),
    DataTypeCodeEditorHelp = require('./data_type_code_editor_help'),
    DataTypeDataSummary = require('./data_type_data_summary'),
    DataTypeResultConfig = require('./data_type_result_config'),
    DataTypeSchemaConfig = require('./data_type_schema_config'),
    DataTypeSourceConfig = require('./data_type_source_config'),
    ID = require('../lib/id'),
    RequiredAWSConfig = require('../models/required_aws_config'),
    SectionHeading = require('../shared_ui/section_heading'),
    SequentialName = require('../lib/sequential_name'),
    BehaviorConfig = require('../models/behavior_config'),
    BehaviorGroup = require('../models/behavior_group'),
    BehaviorVersion = require('../models/behavior_version'),
    DataTypeField = require('../models/data_type_field'),
    Input = require('../models/input'),
    ParamType = require('../models/param_type'),
    ImmutableObjectUtils = require('../lib/immutable_object_utils'),
    autobind = require('../lib/autobind');

  class DataTypeEditor extends React.Component {
    constructor(props) {
      super(props);
      this.state = {
        dataTypeSourceChosen: !this.props.behaviorVersion.isNew
      };
      autobind(this);
    }

    componentWillReceiveProps(nextProps) {
      if (nextProps.behaviorVersion.id !== this.props.behaviorVersion.id) {
        this.setState({
          dataTypeSourceChosen: !nextProps.behaviorVersion.isNew
        });
      }
    }

    dataTypeSourceChosen() {
      return this.state.dataTypeSourceChosen;
    }

    getSelectedBehavior() {
      return this.props.behaviorVersion;
    }

    getBehaviorGroup() {
      return this.props.group;
    }

    getDefaultFieldType() {
      return this.props.paramTypes.find(ea => ea.id === "Text");
    }

    updateDataTypeSource(usesCode) {
      const textType = this.getDefaultFieldType();
      const newConfig = this.getDataTypeConfig().clone({ usesCode: usesCode }).withRequiredFieldsEnsured(textType);
      this.setDataTypeConfig(newConfig);
      this.setState({
        dataTypeSourceChosen: true
      });
    }

    getDataTypeConfig() {
      return this.getSelectedBehavior().getDataTypeConfig();
    }

    usesCode() {
      return this.getDataTypeConfig().usesCode;
    }

    setDataTypeConfig(newConfig, callback) {
      this.props.onChangeConfig({
        dataTypeConfig: newConfig
      }, callback);
    }

    getDataTypeFields() {
      return this.getSelectedBehavior().getDataTypeFields();
    }

    setDataTypeFields(newFields, callback) {
      const newConfig = this.getDataTypeConfig().clone({ fields: newFields });
      this.setDataTypeConfig(newConfig, callback);
    }

    addDataTypeField(field) {
      const newFields = this.getDataTypeFields().concat([field]);
      this.setDataTypeFields(newFields, () => {
        if (this.schemaConfig) {
          this.schemaConfig.focusOnLastField();
        }
      });
    }

    focusOnFirstBlankField() {
      if (this.schemaConfig) {
        this.schemaConfig.focusOnFirstBlankField();
      }
    }

    focusOnDuplicateField() {
      if (this.schemaConfig) {
        this.schemaConfig.focusOnFirstDuplicateField();
      }
    }

    updateDataTypeFieldAtIndexWith(index, newField) {
      var fields = this.getDataTypeFields();
      var newFields = ImmutableObjectUtils.arrayWithNewElementAtIndex(fields, newField, index);

      this.setDataTypeFields(newFields);
    }

    deleteDataTypeFieldAtIndex(index) {
      this.setDataTypeFields(ImmutableObjectUtils.arrayRemoveElementAtIndex(this.getDataTypeFields(), index));
    }

    addNewDataTypeField() {
      const newName = SequentialName.nextFor(this.getDataTypeFields().slice(1), (ea) => ea.name, "field");
      this.addDataTypeField(new DataTypeField({
        fieldId: ID.next(),
        name: newName,
        fieldType: this.getDefaultFieldType()
      }));
    }

    hasInputNamed(name) {
      return this.props.inputs.some(ea => ea.name === name);
    }

    updateDataTypeResultConfig(shouldUseSearch) {
      if (shouldUseSearch) {
        this.props.onAddNewInput('searchQuery');
      } else {
        this.props.onDeleteInputs();
      }
    }

    isFinishedBehavior() {
      const selected = this.getSelectedBehavior();
      return Boolean(!selected.isNew && selected.getFunctionBody());
    }

    isModified() {
      return this.props.isModified(this.props.behaviorVersion);
    }

    isValidForDataStorage() {
      return this.getBehaviorGroup().isValidForDataStorage();
    }

    changeSource() {
      this.setState({
        dataTypeSourceChosen: false
      });
    }

    addDataStorageItems() {
      this.props.onToggleActivePanel('addDataStorageItems', true);
    }

    browseDataStorageItems() {
      this.props.onToggleActivePanel('browseDataStorage', true);
    }

    renderCodeEditor() {
      return (
        <div>
          <CodeConfiguration
            sectionNumber={"3"}
            sectionHeading={"Run code to generate a list"}
            codeEditorHelp={(
              <div className="mbxl">
                <DataTypeCodeEditorHelp
                  functionBody={this.getSelectedBehavior().getFunctionBody()}
                  usesSearch={this.hasInputNamed('searchQuery')}
                  isFinishedBehavior={this.isFinishedBehavior()}
                />
              </div>
            )}

            activePanelName={this.props.activePanelName}
            activeDropdownName={this.props.activeDropdownName}
            onToggleActiveDropdown={this.props.onToggleActiveDropdown}
            onToggleActivePanel={this.props.onToggleActivePanel}
            animationIsDisabled={this.props.animationIsDisabled}

            onToggleAWSConfig={this.props.onToggleAWSConfig}
            behaviorConfig={this.props.behaviorConfig}
            onAWSAddNewEnvVariable={this.props.onAWSAddNewEnvVariable}
            onAWSConfigChange={this.props.onAWSConfigChange}

            apiSelector={this.props.apiSelector}

            inputs={this.props.inputs}
            systemParams={this.props.systemParams}
            requiredAWSConfigs={this.props.requiredAWSConfigs}
            apiApplications={this.props.apiApplications}

            functionBody={this.getSelectedBehavior().getFunctionBody()}
            onChangeFunctionBody={this.props.onChangeCode}
            onCursorChange={this.props.onCursorChange}
            useLineWrapping={this.props.useLineWrapping}
            onToggleCodeEditorLineWrapping={this.props.onToggleCodeEditorLineWrapping}
            canDeleteFunctionBody={false}
            onDeleteFunctionBody={() => null}

            envVariableNames={this.props.envVariableNames}
          />
        </div>
      );
    }

    render() {
      return (
        <div>

          <Collapsible revealWhen={!this.dataTypeSourceChosen()} animationDisabled={this.props.animationIsDisabled}>
            <DataTypeSourceConfig onChange={this.updateDataTypeSource} />
          </Collapsible>

          <Collapsible revealWhen={this.dataTypeSourceChosen()} animationDisabled={this.props.animationIsDisabled}>

            <div className="container ptxl pbs">
              <SectionHeading number="1">
                <span className="mrm align-m">
                  <span>Data source: </span>
                  <span className="type-regular">{this.usesCode() ? "Generated by code" : "Stored on Ellipsis"}</span>
                </span>
                <button type="button" className="button-s button-shrink align-m" onClick={this.changeSource}>Modify</button>
              </SectionHeading>

              {this.usesCode() ? null : (
                <DataTypeDataSummary
                  isModified={this.isModified()}
                  isValid={this.isValidForDataStorage()}
                  onAddItems={this.addDataStorageItems}
                  onBrowse={this.browseDataStorageItems}
                />
              )}
            </div>

            <hr className="man thin bg-gray-light" />

            {this.usesCode() ? (
              <DataTypeResultConfig
                usesSearch={this.hasInputNamed('searchQuery')}
                onChange={this.updateDataTypeResultConfig}
                isFinishedBehavior={this.isFinishedBehavior()}
              />
            ) : (
              <DataTypeSchemaConfig
                ref={(element) => this.schemaConfig = element}
                onChange={this.updateDataTypeFieldAtIndexWith}
                onDelete={this.deleteDataTypeFieldAtIndex}
                onAdd={this.addNewDataTypeField}
                behaviorVersionId={this.props.behaviorVersion.id}
                fields={this.getDataTypeFields()}
                paramTypes={this.props.paramTypes}
                animationDisabled={this.props.animationIsDisabled}
                onConfigureType={this.props.onConfigureType}
              />
            )}

            <hr className="man thin bg-gray-light" />

            {this.usesCode() ? this.renderCodeEditor() : null
            /* Disabled data collection config settings until operative (
              <DataTypeDataCollectionConfig />
            )*/}
          </Collapsible>
        </div>
      );
    }
  }

  DataTypeEditor.propTypes = {
    group: React.PropTypes.instanceOf(BehaviorGroup).isRequired,
    behaviorVersion: React.PropTypes.instanceOf(BehaviorVersion).isRequired,
    paramTypes: React.PropTypes.arrayOf(React.PropTypes.instanceOf(ParamType)).isRequired,
    inputs: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Input)).isRequired,
    onChangeConfig: React.PropTypes.func.isRequired,
    onChangeCode: React.PropTypes.func.isRequired,
    onAddNewInput: React.PropTypes.func.isRequired,
    onDeleteInputs: React.PropTypes.func.isRequired,
    onConfigureType: React.PropTypes.func.isRequired,
    isModified: React.PropTypes.func.isRequired,

    activePanelName: React.PropTypes.string,
    activeDropdownName: React.PropTypes.string,
    onToggleActiveDropdown: React.PropTypes.func.isRequired,
    onToggleActivePanel: React.PropTypes.func.isRequired,
    animationIsDisabled: React.PropTypes.bool,

    behaviorConfig: React.PropTypes.instanceOf(BehaviorConfig).isRequired,

    apiSelector: React.PropTypes.node.isRequired,
    systemParams: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,

    requiredAWSConfigs: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredAWSConfig)).isRequired,

    apiApplications: React.PropTypes.arrayOf(React.PropTypes.shape({
      apiId: React.PropTypes.string.isRequired,
      recommendedScope: React.PropTypes.string,
      application: React.PropTypes.shape({
        applicationId: React.PropTypes.string.isRequired,
        displayName: React.PropTypes.string.isRequired
      })
    })).isRequired,

    onCursorChange: React.PropTypes.func.isRequired,
    useLineWrapping: React.PropTypes.bool.isRequired,
    onToggleCodeEditorLineWrapping: React.PropTypes.func.isRequired,

    envVariableNames: React.PropTypes.arrayOf(React.PropTypes.string).isRequired
  };

  return DataTypeEditor;
});
