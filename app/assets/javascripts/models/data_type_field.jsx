define(function() {

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

    static fieldsFromJson(jsonArray) {
      return jsonArray.map((ea) => new DataTypeField(ea));
    }

  }

  return DataTypeField;
});
