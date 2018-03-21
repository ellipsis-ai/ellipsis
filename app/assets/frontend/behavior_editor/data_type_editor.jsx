import * as React from 'react';
import CodeConfiguration from './code_configuration';
import Collapsible from '../shared_ui/collapsible';
import DataTypeDataSummary from './data_type_data_summary';
import DataTypeResultConfig from './data_type_result_config';
import DataTypeSchemaConfig from './data_type_schema_config';
import DataTypeSourceConfig from './data_type_source_config';
import ID from '../lib/id';
import {RequiredAWSConfig} from '../models/aws';
import {RequiredOAuth2Application} from '../models/oauth2';
import SequentialName from '../lib/sequential_name';
import BehaviorConfig from '../models/behavior_config';
import BehaviorGroup from '../models/behavior_group';
import BehaviorVersion from '../models/behavior_version';
import DataTypeField from '../models/data_type_field';
import Input from '../models/input';
import ParamType from '../models/param_type';
import ImmutableObjectUtils from '../lib/immutable_object_utils';
import autobind from '../lib/autobind';
import SectionHeading from "../shared_ui/section_heading";

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
      }, () => {
        const dataType = this.props.behaviorVersion;
        if (usesCode && !dataType.getFunctionBody()) {
          this.props.onChangeCode(BehaviorVersion.defaultDataTypeCode());
        }
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
      this.addDataTypeField(DataTypeField.fromProps({
        fieldId: ID.next(),
        name: newName,
        fieldType: this.getDefaultFieldType()
      }));
    }

    hasInputNamed(name) {
      return this.props.inputs.some(ea => ea.name === name);
    }

    updateDataTypeResultConfig(shouldUseSearch) {
      const code = this.props.behaviorVersion.getFunctionBody();
      const callback = () => {
        if (!code || code === BehaviorVersion.defaultDataTypeCode(!shouldUseSearch)) {
          this.props.onChangeCode(BehaviorVersion.defaultDataTypeCode(shouldUseSearch));
        }
      };
      if (shouldUseSearch) {
        this.props.onAddNewInput('searchQuery', callback);
      } else {
        this.props.onDeleteInputs(callback);
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
            codeHelpPanelName='helpForBehaviorCode'

            activePanelName={this.props.activePanelName}
            activeDropdownName={this.props.activeDropdownName}
            onToggleActiveDropdown={this.props.onToggleActiveDropdown}
            onToggleActivePanel={this.props.onToggleActivePanel}
            animationIsDisabled={this.props.animationIsDisabled}

            behaviorConfig={this.props.behaviorConfig}

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
            <DataTypeSourceConfig
              onChange={this.updateDataTypeSource}
              onToggleActivePanel={this.props.onToggleActivePanel}
              activePanelName={this.props.activePanelName}
            />
          </Collapsible>

          <Collapsible revealWhen={this.dataTypeSourceChosen()} animationDisabled={this.props.animationIsDisabled}>

            <div className="container ptxl pbs">
              <SectionHeading number="1">
                <span className="mrm align-m">
                  <span>Data source: </span>
                  <span className="type-regular">{this.usesCode() ? "Returned by code" : "Stored on Ellipsis"}</span>
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

            <hr className="man rule-subtle" />

            {this.usesCode() ? (
              <DataTypeResultConfig
                usesSearch={this.hasInputNamed('searchQuery')}
                onChange={this.updateDataTypeResultConfig}
                isFinishedBehavior={this.isFinishedBehavior()}
                activePanelName={this.props.activePanelName}
                onToggleActivePanel={this.props.onToggleActivePanel}
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

            <hr className="man rule-subtle" />

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

    systemParams: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,

    requiredAWSConfigs: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredAWSConfig)).isRequired,

    apiApplications: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredOAuth2Application)).isRequired,

    onCursorChange: React.PropTypes.func.isRequired,
    useLineWrapping: React.PropTypes.bool.isRequired,
    onToggleCodeEditorLineWrapping: React.PropTypes.func.isRequired,

    envVariableNames: React.PropTypes.arrayOf(React.PropTypes.string).isRequired
};

export default DataTypeEditor;

