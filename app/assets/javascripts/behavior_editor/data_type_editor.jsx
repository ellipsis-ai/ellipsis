define(function(require) {
  var React = require('react'),
    CodeConfiguration = require('./code_configuration'),
    Collapsible = require('../shared_ui/collapsible'),
    DataTypeCodeEditorHelp = require('./data_type_code_editor_help'),
    DataTypeDataCollectionConfig = require('./data_type_data_collection_config'),
    DataTypeResultConfig = require('./data_type_result_config'),
    DataTypeSchemaConfig = require('./data_type_schema_config'),
    DataTypeSourceConfig = require('./data_type_source_config'),
    SectionHeading = require('../shared_ui/section_heading'),
    SequentialName = require('../lib/sequential_name'),
    BehaviorVersion = require('../models/behavior_version'),
    DataTypeField = require('../models/data_type_field'),
    Input = require('../models/input'),
    ImmutableObjectUtils = require('../lib/immutable_object_utils');

  return React.createClass({
    displayName: 'DataTypeEditor',
    propTypes: {
      behaviorVersion: React.PropTypes.instanceOf(BehaviorVersion).isRequired,
      paramTypes: React.PropTypes.arrayOf(
        React.PropTypes.shape({
          id: React.PropTypes.string.isRequired,
          name: React.PropTypes.string.isRequired
        })
      ).isRequired,
      inputs: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Input)).isRequired,
      onChange: React.PropTypes.func.isRequired,
      onAddNewInput: React.PropTypes.func.isRequired,
      onConfigureType: React.PropTypes.func.isRequired,

      activePanelName: React.PropTypes.string,
      activeDropdownName: React.PropTypes.string,
      onToggleActiveDropdown: React.PropTypes.func.isRequired,
      onToggleActivePanel: React.PropTypes.func.isRequired,
      animationIsDisabled: React.PropTypes.bool,

      onToggleAWSConfig: React.PropTypes.func.isRequired,
      awsConfig: React.PropTypes.object,
      onAWSAddNewEnvVariable: React.PropTypes.func.isRequired,
      onAWSConfigChange: React.PropTypes.func.isRequired,

      apiSelector: React.PropTypes.node.isRequired,
      systemParams: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
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
    },

    getInitialState: function() {
      return {
        dataTypeSourceChosen: !this.props.behaviorVersion.isNew
      };
    },

    componentWillReceiveProps(nextProps) {
      if (nextProps.behaviorVersion.id !== this.props.behaviorVersion.id) {
        this.setState({
          dataTypeSourceChosen: !nextProps.behaviorVersion.isNew
        });
      }
    },

    dataTypeSourceChosen: function() {
      return this.state.dataTypeSourceChosen;
    },

    getSelectedBehavior: function() {
      return this.props.behaviorVersion;
    },

    updateDataTypeSource: function(usesCode) {
      const textType = this.props.paramTypes.find(ea => ea.id === "Text");
      const newConfig = this.getDataTypeConfig().clone({ usesCode: usesCode }).withBuiltinFieldsEnsured(textType);
      this.setDataTypeConfig(newConfig);
      this.setState({
        dataTypeSourceChosen: true
      });
      if (newConfig.fields.length === 0) {
        this.addNewDataTypeField();
      }
    },

    getDataTypeConfig: function() {
      return this.getSelectedBehavior().getDataTypeConfig();
    },

    usesCode: function() {
      return this.getDataTypeConfig().usesCode;
    },

    setDataTypeConfig: function(newConfig, callback) {
      this.props.onChange({
        dataTypeConfig: newConfig
      }, callback);
    },

    getDataTypeFields: function() {
      return this.getSelectedBehavior().getDataTypeFields();
    },

    setDataTypeFields: function(newFields, callback) {
      const newConfig = this.getDataTypeConfig().clone({ fields: newFields });
      this.setDataTypeConfig(newConfig, callback);
    },

    addDataTypeField: function(field) {
      const newFields = this.getDataTypeFields().concat([field]);
      this.setDataTypeFields(newFields, () => {
        if (this.schemaConfig) {
          this.schemaConfig.focusOnLastField();
        }
      });
    },

    focusOnFirstBlankField: function() {
      if (this.schemaConfig) {
        this.schemaConfig.focusOnFirstBlankField();
      }
    },

    updateDataTypeFieldAtIndexWith: function(index, newField) {
      var fields = this.getDataTypeFields();
      var newFields = ImmutableObjectUtils.arrayWithNewElementAtIndex(fields, newField, index);

      this.setDataTypeFields(newFields);
    },

    deleteDataTypeFieldAtIndex: function(index) {
      this.setDataTypeFields(ImmutableObjectUtils.arrayRemoveElementAtIndex(this.getDataTypeFields(), index));
    },

    addNewDataTypeField: function() {
      const newName = SequentialName.nextFor(this.getDataTypeFields(), (ea) => ea.name, "field");
      const url = jsRoutes.controllers.BehaviorEditorController.newUnsavedDataTypeField(newName).url;
      fetch(url, { credentials: 'same-origin' })
        .then(response => response.json())
        .then(json => {
          this.addDataTypeField(new DataTypeField(json));
        });
    },

    hasInputNamed: function(name) {
      return this.props.inputs.some(ea => ea.name === name);
    },

    updateDataTypeResultConfig: function(shouldUseSearch) {
      if (shouldUseSearch) {
        this.props.onAddNewInput('searchQuery');
      } else {
        this.props.onChange({
          inputIds: []
        });
      }
    },

    isFinishedBehavior: function() {
      const selected = this.getSelectedBehavior();
      return Boolean(!selected.isNew && selected.getFunctionBody());
    },

    updateCode: function(newCode) {
      this.props.onChange({
        functionBody: newCode
      });
    },

    changeSource: function() {
      this.setState({
        dataTypeSourceChosen: false
      });
    },

    renderCodeEditor: function() {
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
            awsConfig={this.props.awsConfig}
            onAWSAddNewEnvVariable={this.props.onAWSAddNewEnvVariable}
            onAWSConfigChange={this.props.onAWSConfigChange}

            apiSelector={this.props.apiSelector}

            inputs={this.props.inputs}
            systemParams={this.props.systemParams}
            apiApplications={this.props.apiApplications}

            functionBody={this.getSelectedBehavior().getFunctionBody()}
            onChangeFunctionBody={this.updateCode}
            onCursorChange={this.props.onCursorChange}
            useLineWrapping={this.props.useLineWrapping}
            onToggleCodeEditorLineWrapping={this.props.onToggleCodeEditorLineWrapping}
            canDeleteFunctionBody={false}
            onDeleteFunctionBody={() => null}

            envVariableNames={this.props.envVariableNames}
          />
        </div>
      );
    },

    render: function() {
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
                fields={this.getDataTypeFields()}
                paramTypes={this.props.paramTypes}
                animationDisabled={this.props.animationIsDisabled}
                onConfigureType={this.props.onConfigureType}
              />
            )}

            <hr className="man thin bg-gray-light" />

            {this.usesCode() ? this.renderCodeEditor() : (
              <DataTypeDataCollectionConfig />
            )}
          </Collapsible>
        </div>
      );
    }
  });
});
