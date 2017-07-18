define(function(require) {

  const DataTypeField = require('./data_type_field'),
    SequentialName = require("../lib/sequential_name"),
    ID = require("../lib/id");

  class DataTypeConfig {
    constructor(props) {
      var initialProps = Object.assign({
        fields: [],
        usesCode: true
      }, props);
      Object.defineProperties(this, {
        fields: {
          value: initialProps.fields,
          enumerable: true
        },
        usesCode: {
          value: initialProps.usesCode,
          enumerable: true
        }
      });
    }

    requiresFields() {
      return !this.usesCode;
    }

    hasTextFields() {
      return this.fields.some((ea) => ea.name !== "id" && ea.fieldType.id === "Text");
    }

    isIdField(field, index) {
      return field.name === "id" && index === DataTypeConfig.ID_FIELD_INDEX;
    }

    hasIdField() {
      return this.fields.some(this.isIdField);
    }

    withRequiredFieldsEnsured(requiredFieldType) {
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

    isMissingFields() {
      return this.requiresFields() && !this.hasTextFields();
    }

    isValidForDataStorage() {
      return this.hasIdField() && this.hasTextFields() && this.fields.every((ea) => ea.name.length > 0);
    }

    getFields() {
      return this.fields;
    }

    getWritableFields() {
      const fields = this.getFields();
      if (fields.length > 0) {
        return fields.filter((ea, index) => !this.isIdField(ea, index));
      } else {
        return [];
      }
    }

    clone(props) {
      return new DataTypeConfig(Object.assign({}, this, props));
    }

    static fromJson(props) {
      const materializedProps = Object.assign({}, props);
      if (props.fields) {
        materializedProps.fields = DataTypeField.fieldsFromJson(props.fields);
      }
      return new DataTypeConfig(materializedProps);
    }

  }

  DataTypeConfig.ID_FIELD_INDEX = 0;

  return DataTypeConfig;
});
