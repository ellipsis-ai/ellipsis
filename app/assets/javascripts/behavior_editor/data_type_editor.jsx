define(function(require) {
  var React = require('react'),
    CodeConfiguration = require('./code_configuration'),
    DataTypeCodeEditorHelp = require('./data_type_code_editor_help'),
    DataTypeResultConfig = require('./data_type_result_config'),
    DataTypeSchemaConfig = require('./data_type_schema_config'),
    DataTypeSourceConfig = require('./data_type_source_config'),
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

    getSelectedBehavior: function() {
      return this.props.behaviorVersion;
    },

    updateDataTypeSource: function(usesCode) {
      const newConfig = this.getDataTypeConfig().clone({ usesCode: usesCode });
      this.setDataTypeConfig(newConfig);
    },

    getDataTypeConfig: function() {
      return this.getSelectedBehavior().getDataTypeConfig();
    },

    usesCode: function() {
      return this.getDataTypeConfig().usesCode;
    },

    setDataTypeConfig: function(newConfig) {
      this.props.onChange({
        dataTypeConfig: newConfig
      });
    },

    getDataTypeFields: function() {
      return this.getSelectedBehavior().getDataTypeFields();
    },

    setDataTypeFields: function(newFields) {
      const newConfig = this.getDataTypeConfig().clone({ fields: newFields });
      this.setDataTypeConfig(newConfig);
    },

    addDataTypeField: function(field) {
      const newFields = this.getDataTypeFields().concat([field]);
      this.setDataTypeFields(newFields);
    },

    updateDataTypeFieldAtIndexWith: function(index, newField) {
      var fields = this.getDataTypeFields();
      var newFields = ImmutableObjectUtils.arrayWithNewElementAtIndex(fields, newField, index);

      this.setDataTypeFields(newFields);
    },

    deleteDataTypeFieldAtIndex: function(index) {
      this.setDataTypeFields(ImmutableObjectUtils.arrayRemoveElementAtIndex(this.getDataTypeFields(), index));
    },

    getNewGenericDataTypeFieldName: function() {
      let newIndex = this.getDataTypeFields().length + 1;
      while (this.getDataTypeFields().some(ea => {
        return ea.name === 'dataTypeField' + newIndex;
      })) {
        newIndex++;
      }
      return `dataTypeField${newIndex}`;
    },

    addNewDataTypeField: function(optionalNewName) {
      const newName = optionalNewName || this.getNewGenericDataTypeFieldName();
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

    render: function() {
      return (
        <div>
          <DataTypeSourceConfig
            usesCode={this.usesCode()}
            onChange={this.updateDataTypeSource}
            isFinishedBehavior={this.isFinishedBehavior()}
          />

          {this.usesCode() ? (
            <DataTypeResultConfig
              usesSearch={this.hasInputNamed('searchQuery')}
              onChange={this.updateDataTypeResultConfig}
              isFinishedBehavior={this.isFinishedBehavior()}
            />
          ) : (
            <DataTypeSchemaConfig
              ref="DataTypeSchemaConfig"
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

          {this.usesCode() ? (
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
          ) : null}
        </div>
      );
    }
  });
});
