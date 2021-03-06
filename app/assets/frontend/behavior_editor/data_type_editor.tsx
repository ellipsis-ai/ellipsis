import * as React from 'react';
import CodeConfiguration from './code_configuration';
import Collapsible from '../shared_ui/collapsible';
import DataTypeDataSummary from './data_type_data_summary';
import UserInputConfiguration from './user_input_configuration';
import DataTypeSchemaConfig from './data_type_schema_config';
import DataTypeSourceConfig from './data_type_source_config';
import ID from '../lib/id';
import SequentialName from '../lib/sequential_name';
import {BehaviorConfigInterface} from '../models/behavior_config';
import BehaviorGroup from '../models/behavior_group';
import BehaviorVersion from '../models/behavior_version';
import DataTypeField from '../models/data_type_field';
import Input from '../models/input';
import ParamType from '../models/param_type';
import ImmutableObjectUtils from '../lib/immutable_object_utils';
import autobind from '../lib/autobind';
import SectionHeading from "../shared_ui/section_heading";
import Editable from "../models/editable";
import DataTypeConfig from "../models/data_type_config";
import Button from "../form/button";

interface Props {
  group: BehaviorGroup
  behaviorVersion: BehaviorVersion
  paramTypes: Array<ParamType>
  builtinParamTypes: Array<ParamType>
  inputs: Array<Input>
  onChange: (newConfigProps: Option<Partial<BehaviorConfigInterface>>, newCode: Option<string>, optionalCallback?: () => void) => void
  onConfigureType: (paramTypeId: string) => void
  isModified: (editable: Editable) => boolean

  activePanelName: string
  activeDropdownName: string
  onToggleActiveDropdown: (dropdownName: string) => void
  onToggleActivePanel: (panelName: string, beModal?: boolean, optionalCallback?: () => void) => void
  animationIsDisabled: boolean

  userInputConfiguration: React.ReactNode
  codeConfiguration: React.ReactNode
}

interface DataTypeEditorSettings {
  code: string
  fields: Array<DataTypeField>
}

interface State {
  prevSettings: Option<DataTypeEditorSettings>
}

class DataTypeEditor extends React.Component<Props, State> {
    schemaConfig: Option<DataTypeSchemaConfig>;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.state = {
        prevSettings: null
      };
    }

    dataTypeSourceChosen(): boolean {
      return !this.props.behaviorVersion.hasDefaultDataTypeSettings();
    }

    getSelectedBehavior(): BehaviorVersion {
      return this.props.behaviorVersion;
    }

    getBehaviorGroup(): BehaviorGroup {
      return this.props.group;
    }

    getDefaultFieldType(): Option<ParamType> {
      return this.props.paramTypes.find(ea => ea.id === "Text");
    }

    getNewSettingsForSource(usesCode: boolean): DataTypeEditorSettings {
      const prevSettings = this.state.prevSettings;
      const fieldsToUse = prevSettings ? prevSettings.fields : [];
      const codeToUse = prevSettings ? prevSettings.code : "";
      if (usesCode) {
        return {
          code: codeToUse || BehaviorVersion.defaultDataTypeCode(),
          fields: []
        };
      } else {
        return {
          code: "",
          fields: fieldsToUse
        };
      }
    }

    updateDataTypeSource(usesCode: boolean): void {
      const textType = this.getDefaultFieldType();
      const config = this.getDataTypeConfig();
      if (textType && config) {
        const settings = this.getNewSettingsForSource(usesCode);
        const newConfig = config.clone({
          usesCode: usesCode,
          fields: settings.fields
        }).withRequiredFieldsEnsured(textType);
        this.props.onChange({ dataTypeConfig: newConfig }, settings.code);
      }
    }

    getDataTypeConfig(): Option<DataTypeConfig> {
      return this.getSelectedBehavior().getDataTypeConfig();
    }

    usesCode(): boolean {
      const config = this.getDataTypeConfig();
      return Boolean(config && config.usesCode);
    }

    getDataTypeFields(): Array<DataTypeField> {
      return this.getSelectedBehavior().getDataTypeFields();
    }

    setDataTypeFields(newFields: Array<DataTypeField>, callback?: () => void): void {
      const config = this.getDataTypeConfig();
      const newConfig = config ? config.clone({ fields: newFields }) : null;
      if (newConfig) {
        this.props.onChange({ dataTypeConfig: newConfig }, null, callback);
      }
    }

    addDataTypeField(field: DataTypeField): void {
      const newFields = this.getDataTypeFields().concat([field]);
      this.setDataTypeFields(newFields, () => {
        if (this.schemaConfig) {
          this.schemaConfig.focusOnLastField();
        }
      });
    }

    focusOnFirstBlankField(): void {
      if (this.schemaConfig) {
        this.schemaConfig.focusOnFirstBlankField();
      }
    }

    focusOnDuplicateField(): void {
      if (this.schemaConfig) {
        this.schemaConfig.focusOnFirstDuplicateField();
      }
    }

    updateDataTypeFieldAtIndexWith(index: number, newField: DataTypeField): void {
      var fields = this.getDataTypeFields();
      var newFields = ImmutableObjectUtils.arrayWithNewElementAtIndex(fields, newField, index);

      this.setDataTypeFields(newFields);
    }

    deleteDataTypeFieldAtIndex(index: number): void {
      this.setDataTypeFields(ImmutableObjectUtils.arrayRemoveElementAtIndex(this.getDataTypeFields(), index));
    }

    addNewDataTypeField(): void {
      const newName = SequentialName.nextFor(this.getDataTypeFields().slice(1), (ea) => ea.name, "field");
      const defaultType = this.getDefaultFieldType();
      if (defaultType) {
        this.addDataTypeField(DataTypeField.fromProps({
          fieldId: ID.next(),
          name: newName,
          fieldType: defaultType
        }));
      }
    }

    isModified(): boolean {
      return this.props.isModified(this.props.behaviorVersion);
    }

    isValidForDataStorage(): boolean {
      return this.getBehaviorGroup().isValidForDataStorage();
    }

    resetSource(): void {
      this.setState({
        prevSettings: {
          code: this.getSelectedBehavior().getFunctionBody(),
          fields: this.getDataTypeFields()
        }
      }, () => {
        this.props.onChange({ dataTypeConfig: DataTypeConfig.defaultConfig() }, "");
      });
    }

    addDataStorageItems(): void {
      this.props.onToggleActivePanel('addDataStorageItems', true);
    }

    browseDataStorageItems(): void {
      this.props.onToggleActivePanel('browseDataStorage', true);
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

            <div className="container ptxl mbxl">
              <SectionHeading number="1">
                <span className="mrm align-m">
                  <span>Data source: </span>
                  <span className="type-regular">{this.usesCode() ? "Returned by code" : "Stored on Ellipsis"}</span>
                </span>
                <Button className="button-s button-shrink align-m" onClick={this.resetSource}>Modify</Button>
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

            {this.usesCode() ? this.props.userInputConfiguration : (
              <DataTypeSchemaConfig
                ref={(element) => this.schemaConfig = element}
                onChange={this.updateDataTypeFieldAtIndexWith}
                onDelete={this.deleteDataTypeFieldAtIndex}
                onAdd={this.addNewDataTypeField}
                behaviorVersionId={this.props.behaviorVersion.id}
                fields={this.getDataTypeFields()}
                paramTypes={this.props.builtinParamTypes}
                animationDisabled={this.props.animationIsDisabled}
                onConfigureType={this.props.onConfigureType}
              />
            )}

            <hr className="man rule-subtle" />

            {this.props.codeConfiguration}
          </Collapsible>
        </div>
      );
    }
}

export default DataTypeEditor;

