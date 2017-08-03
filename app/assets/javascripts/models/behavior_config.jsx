define(function(require) {
  var
    DataTypeConfig = require('./data_type_config');

  class BehaviorConfig {
    constructor(props) {
      var initialProps = Object.assign({
        isDataType: false,
        dataTypeConfig: null,
      }, props);

      Object.defineProperties(this, {
        isDataType: { value: initialProps.isDataType, enumerable: true },
        dataTypeConfig: { value: initialProps.dataTypeConfig, enumerable: true }
      });
    }

    getDataTypeConfig() {
      return this.dataTypeConfig;
    }

    getDataTypeFields() {
      return this.getDataTypeConfig() ? this.getDataTypeConfig().getFields() : [];
    }

    clone(props) {
      return new BehaviorConfig(Object.assign({}, this, props));
    }

    static fromJson(props) {
      const materializedProps = Object.assign({}, props);
      if (props.dataTypeConfig) {
        materializedProps.dataTypeConfig = DataTypeConfig.fromJson(props.dataTypeConfig);
      }
      return new BehaviorConfig(materializedProps);
    }

  }

  return BehaviorConfig;
});
