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

    hasIdField() {
      return this.fields.some((ea, index) => ea.name === "id" && index === DataTypeConfig.ID_FIELD_INDEX);
    }

    withRequiredFieldsEnsured(requiredFieldType) {
      if (!this.requiresFields()) {
        return this;
      }

      const missingFields = [];
      if (!this.hasIdField()) {
        missingFields.push(new DataTypeField({ name: "id", fieldId: "id", fieldType: requiredFieldType }));
      }
      if (!this.hasTextFields()) {
        const newName = SequentialName.nextFor(this.fields, (ea) => ea.name, "field");
        missingFields.push(new DataTypeField({ name: newName, fieldId: ID.next(), fieldType: requiredFieldType }));
      }
      return this.clone({ fields: missingFields.concat(this.fields) });
    }

    isMissingFields() {
      return this.requiresFields() && !this.hasTextFields();
    }

    getFields() {
      return this.fields;
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
