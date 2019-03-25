import DataTypeField, {DataTypeFieldJson} from './data_type_field';
import SequentialName from "../lib/sequential_name";
import ID from "../lib/id";
import ParamType from "./param_type";

const ID_FIELD_INDEX = 0;

export interface DataTypeConfigJson {
  fields: Array<DataTypeFieldJson>;
  usesCode: boolean;
}

interface DataTypeConfigInterface extends DataTypeConfigJson {
  fields: Array<DataTypeField>
}

class DataTypeConfig implements DataTypeConfigInterface {
  readonly usesCode: boolean;

  constructor(
    readonly fields: Array<DataTypeField>,
    usesCode: Option<boolean>
  ) {
      Object.defineProperties(this, {
        fields: {
          value: fields,
          enumerable: true
        },
        usesCode: {
          value: usesCode === undefined ? true : !!usesCode,
          enumerable: true
        }
      });
  }

    requiresFields(): boolean {
      return !this.usesCode;
    }

    hasTextFields(): boolean {
      return this.fields.some((ea) => ea.name !== "id" && ea.fieldType.id === "Text");
    }

    isIdField(field: DataTypeField, index: number): boolean {
      return field.name === "id" && index === ID_FIELD_INDEX;
    }

    hasIdField(): boolean {
      return this.fields.some(this.isIdField);
    }

    withRequiredFieldsEnsured(requiredFieldType: ParamType): DataTypeConfig {
      if (!this.requiresFields()) {
        return this;
      }

      let fieldsToUse = this.fields.slice();
      if (!this.hasIdField()) {
        const newIdField = DataTypeField.fromProps({ name: "id", fieldId: "id", fieldType: requiredFieldType });
        fieldsToUse = [newIdField].concat(fieldsToUse);
      }
      if (!this.hasTextFields()) {
        const newName = SequentialName.nextFor(this.fields, (ea) => ea.name, "field");
        fieldsToUse.push(DataTypeField.fromProps({ name: newName, fieldId: ID.next(), fieldType: requiredFieldType }));
      }
      return this.clone({ fields: fieldsToUse });
    }

    isMissingFields(): boolean {
      return this.requiresFields() && !this.hasTextFields();
    }

    isValidForDataStorage(): boolean {
      return this.usesCode || this.hasIdField() && this.hasTextFields() && this.fields.every((ea) => ea.name.length > 0);
    }

    getFields(): Array<DataTypeField> {
      return this.fields;
    }

    getWritableFields(): Array<DataTypeField> {
      const fields = this.getFields();
      if (fields.length > 0) {
        return fields.filter((ea, index) => !this.isIdField(ea, index));
      } else {
        return [];
      }
    }

    clone(props: Partial<DataTypeConfigInterface>): DataTypeConfig {
      return DataTypeConfig.fromProps(Object.assign({}, this, props));
    }

    static defaultConfig(): DataTypeConfig {
      return new DataTypeConfig([], true);
    }

    static fromProps(props: DataTypeConfigInterface): DataTypeConfig {
      return new DataTypeConfig(
        props.fields,
        props.usesCode
      );
    }

    static fromJson(props: DataTypeConfigJson): DataTypeConfig {
      const materializedProps = Object.assign({}, props, props.fields ? {
        fields: DataTypeField.fieldsFromJson(props.fields)
      } : null);
      return DataTypeConfig.fromProps(materializedProps);
    }

  }

export default DataTypeConfig;

