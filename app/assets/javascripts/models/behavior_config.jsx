// @flow
define(function(require) {
  const
    DataTypeConfig = require('./data_type_config');

  class BehaviorConfig {
    exportId: string;
    name: string;
    forcePrivateResponse: boolean;
    isDataType: boolean;
    dataTypeConfig: DataTypeConfig;

    constructor(
      exportId: string,
      name: string,
      forcePrivateResponse: boolean,
      isDataType: boolean,
      dataTypeConfig: DataTypeConfig
    ) {
      Object.defineProperties(this, {
        exportId: { value: exportId, enumerable: true },
        name: { value: name, enumerable: true },
        forcePrivateResponse: { value: forcePrivateResponse, enumerable: true },
        isDataType: { value: isDataType, enumerable: true },
        dataTypeConfig: { value: dataTypeConfig, enumerable: true }
      });
    }

    getDataTypeConfig(): DataTypeConfig {
      return this.dataTypeConfig;
    }

    getDataTypeFields() {
      return this.getDataTypeConfig() ? this.getDataTypeConfig().getFields() : [];
    }

    clone(props): BehaviorConfig {
      return BehaviorConfig.fromProps(Object.assign({}, this, props));
    }

    static fromProps(props): BehaviorConfig {
      return new BehaviorConfig(
        props.exportId,
        props.name,
        props.forcePrivateResponse,
        props.isDataType,
        props.dataTypeConfig
      )
    }

    static fromJson(props): BehaviorConfig {
      const materializedProps = Object.assign({}, props);
      if (props.dataTypeConfig) {
        materializedProps.dataTypeConfig = DataTypeConfig.fromJson(props.dataTypeConfig);
      }
      return BehaviorConfig.fromProps(materializedProps);
    }

  }

  return BehaviorConfig;
});
