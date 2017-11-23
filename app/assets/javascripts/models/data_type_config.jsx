// @flow
define(function(require) {

  const DataTypeField = require('./data_type_field'),
    SequentialName = require("../lib/sequential_name"),
    ID = require("../lib/id");

  const ID_FIELD_INDEX = 0;

  type DataTypeConfigProps = {
    fields: Array<DataTypeField>,
    usesCode: boolean
  };

  class DataTypeConfig {
    fields: Array<DataTypeField>;
    usesCode: boolean;

    constructor(props: DataTypeConfigProps) {
      Object.defineProperties(this, {
        fields: {
          value: props.fields,
          enumerable: true
        },
        usesCode: {
          value: props.usesCode,
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

    isIdField(field, index): boolean {
      return field.name === "id" && index === ID_FIELD_INDEX;
    }

    hasIdField(): boolean {
      return this.fields.some(this.isIdField);
    }

    withRequiredFieldsEnsured(requiredFieldType): DataTypeConfig {
      if (!this.requiresFields()) {
        return this;
      }

      let fieldsToUse = this.fields.slice();
      if (!this.hasIdField()) {
        const newIdField = new DataTypeField({ name: "id", fieldId: "id", fieldType: requiredFieldType });
        fieldsToUse = [newIdField].concat(fieldsToUse);
      }
      if (!this.hasTextFields()) {
        const newName = SequentialName.nextFor(this.fields, (ea) => ea.name, "field");
        fieldsToUse.push(new DataTypeField({ name: newName, fieldId: ID.next(), fieldType: requiredFieldType }));
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

    clone(props): DataTypeConfig {
      return new DataTypeConfig(Object.assign({}, this, props));
    }

    static fromJson(props: DataTypeConfigProps): DataTypeConfig {
      const materializedProps = Object.assign({}, props);
      if (props.fields) {
        materializedProps.fields = DataTypeField.fieldsFromJson(props.fields);
      }
      return new DataTypeConfig(materializedProps);
    }

  }

  return DataTypeConfig;
});
