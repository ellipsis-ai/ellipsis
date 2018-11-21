import * as React from 'react';
import SectionHeading from '../shared_ui/section_heading';
import DataTypeFieldDefinition from './data_type_field_definition';
import ParamType from '../models/param_type';
import autobind from '../lib/autobind';
import DataTypeField from "../models/data_type_field";
import Button from "../form/button";
import Collapsible from "../shared_ui/collapsible";

interface Props {
  onChange: (fieldIndex: number, newField: DataTypeField) => void
  onDelete: (fieldIndex: number) => void
  onAdd: (callback: () => void) => void
  behaviorVersionId: Option<string>
  fields: Array<DataTypeField>
  paramTypes: Array<ParamType>
  animationDisabled?: Option<boolean>
  onConfigureType: (fieldTypeId: string) => void
}

class DataTypeSchemaConfig extends React.Component<Props> {
    fieldComponents: Array<Option<DataTypeFieldDefinition>>;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.fieldComponents = [];
    }

    onChange(index: number, data: DataTypeField): void {
      this.props.onChange(index, data);
    }

    onDelete(index: number): void {
      this.props.onDelete(index);
    }

    addField(): void {
      this.props.onAdd(() => this.focusOnLastField());
    }

    focusOnLastField(): void {
      const lastFieldIndex = this.props.fields.length - 1;
      if (lastFieldIndex >= 0) {
        this.focusIndex(lastFieldIndex);
      }
    }

    focusOnFirstBlankField(): void {
      const index = this.props.fields.findIndex((ea) => !ea.name);
      if (index >= 0) {
        this.focusIndex(index);
      }
    }

    focusOnFirstDuplicateField(): void {
      const dupeIndex = this.props.fields.findIndex((current, index) => Boolean(current.name) && this.props.fields.slice(0, index).some((previous) => previous.name === current.name));
      if (dupeIndex >= 0) {
        this.focusIndex(dupeIndex);
      }
    }

    focusIndex(index: number): void {
      const field = this.fieldComponents[index];
      if (field) {
        field.focus();
      }
    }

    render() {
      return (
        <div className="mtxl">
          <hr className="man rule-subtle" />

          <div className="mtxl columns container container-narrow">
            <div className="mbxxl">
              <div>
                <SectionHeading number="2">Data fields</SectionHeading>
                <div className="mbm">
                  {this.props.fields.map((field, index) => (
                    <div key={`dataTypeField${index}`} className="mbs">
                      <DataTypeFieldDefinition
                        key={'DataTypeFieldDefinition' + index}
                        ref={(component) => this.fieldComponents[index] = component}
                        field={field}
                        isBuiltIn={index === 0}
                        paramTypes={this.props.paramTypes}
                        onChange={this.onChange.bind(this, index)}
                        onDelete={this.onDelete.bind(this, index)}
                        id={`dataTypeFieldDefinition${index}`}
                        onConfigureType={this.props.onConfigureType}
                        behaviorVersionId={this.props.behaviorVersionId}
                      />
                    </div>
                  ))}
                </div>
                <div>
                  <Button className="button-s mrm mbs" onClick={this.addField}>
                    Add another field
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </div>

      );
    }
}

export default DataTypeSchemaConfig;
