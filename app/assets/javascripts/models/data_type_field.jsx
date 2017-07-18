define(function(require) {
  const ParamType = require('./param_type');

  class DataTypeField {
    constructor(props) {
      var initialProps = Object.assign({
        fieldId: null,
        fieldVersionId: null,
        name: "",
        fieldType: null,
        isLabel: false
      }, props);

      if (!initialProps.fieldId) {
        throw new Error("New DataTypeField must have an fieldId property");
      }
      if (!initialProps.fieldType) {
        throw new Error("New DataTypeField object must have a type set");
      }

      Object.defineProperties(this, {
        fieldVersionId: {
          value: initialProps.fieldVersionId,
          emumerable: true
        },
        fieldId: {
          value: initialProps.fieldId,
          enumerable: true
        },
        name: {
          value: initialProps.name,
          enumerable: true
        },
        fieldType: {
          value: initialProps.fieldType,
          enumerable: true
        },
        isLabel: {
          value: initialProps.isLabel,
          enumerable: true
        }
      });
    }

    clone(props) {
      return new DataTypeField(Object.assign({}, this, props));
    }

    static fromJson(props) {
      const materializedProps = Object.assign({}, props);
      if (props.fieldType) {
        materializedProps.fieldType = ParamType.fromJson(props.fieldType);
      }
      return new DataTypeField(materializedProps);
    }

    static fieldsFromJson(jsonArray) {
      return jsonArray.map(DataTypeField.fromJson);
    }

  }

  return DataTypeField;
});
