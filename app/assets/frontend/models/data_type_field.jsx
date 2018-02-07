// @flow
import ParamType from './param_type';

class DataTypeField {
    fieldId: string;
    fieldVersionId: ?string;
    name: string;
    fieldType: ParamType;
    isLabel: boolean;

    constructor(
      fieldId: string,
      fieldVersionId: ?string,
      name: ?string,
      fieldType: ParamType,
      isLabel: ?boolean
    ) {
      Object.defineProperties(this, {
        fieldVersionId: {
          value: fieldVersionId,
          emumerable: true
        },
        fieldId: {
          value: fieldId,
          enumerable: true
        },
        name: {
          value: name || "",
          enumerable: true
        },
        fieldType: {
          value: fieldType,
          enumerable: true
        },
        isLabel: {
          value: !!isLabel,
          enumerable: true
        }
      });
    }

    clone(props: {}): DataTypeField {
      const newProps = Object.assign({}, this, props);
      return DataTypeField.fromProps(newProps);
    }

    static fromProps(props: { fieldId: string, fieldVersionId?: ?string, name: string, fieldType: ParamType, isLabel?: boolean }): DataTypeField {
      return new DataTypeField(
        props.fieldId,
        props.fieldVersionId ? props.fieldVersionId : null,
        props.name,
        props.fieldType,
        props.isLabel ? props.isLabel : false
      );
    }

    static fromJson(props): DataTypeField {
      const materializedProps = Object.assign({}, props);
      if (props.fieldType) {
        materializedProps.fieldType = ParamType.fromJson(props.fieldType);
      }
      return DataTypeField.fromProps(materializedProps);
    }

    static fieldsFromJson(jsonArray): Array<DataTypeField> {
      return jsonArray.map(DataTypeField.fromJson);
    }

  }

export default DataTypeField;

