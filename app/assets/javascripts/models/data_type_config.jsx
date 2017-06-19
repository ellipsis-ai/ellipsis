define(function(require) {

  const DataTypeField = require('./data_type_field');

  class DataTypeConfig {
    constructor(props) {
      var initialProps = Object.assign({
        fields: []
      }, props);
      Object.defineProperties(this, {
        fields: {
          value: initialProps.fields,
          enumerable: true
        }
      });
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

  return DataTypeConfig;
});
