import BehaviorResponseType, {BehaviorResponseTypeJson} from './behavior_response_type';
import DataTypeConfig, {DataTypeConfigJson} from './data_type_config';

export interface BehaviorConfigJson {
  exportId?: Option<string>;
  name?: Option<string>;
  responseType: BehaviorResponseTypeJson;
  canBeMemoized?: Option<boolean>;
  isDataType: boolean;
  isTest: boolean;
  dataTypeConfig?: Option<DataTypeConfigJson>;
}

interface BehaviorConfigInterface extends BehaviorConfigJson {
  dataTypeConfig?: Option<DataTypeConfig>;
  responseType: BehaviorResponseType;
  canBeMemoized: boolean;
}

class BehaviorConfig implements BehaviorConfigInterface {
  constructor(
    readonly exportId: Option<string>,
    readonly name: Option<string>,
    readonly responseType: BehaviorResponseType,
    readonly canBeMemoized: boolean,
    readonly isDataType: boolean,
    readonly isTest: boolean,
    readonly dataTypeConfig: Option<DataTypeConfig>
  ) {
      Object.defineProperties(this, {
        exportId: { value: exportId, enumerable: true },
        name: { value: name, enumerable: true },
        responseType: { value: responseType, enumerable: true },
        canBeMemoized: { value: Boolean(canBeMemoized), enumerable: true },
        isDataType: { value: isDataType, enumerable: true },
        isTest: { value: isTest, enumerable: true },
        dataTypeConfig: { value: dataTypeConfig, enumerable: true }
      });
    }

    getDataTypeConfig(): Option<DataTypeConfig> {
      return this.dataTypeConfig;
    }

    getDataTypeFields() {
      const config = this.getDataTypeConfig();
      return config ? config.getFields() : [];
    }

    clone(props: Partial<BehaviorConfigInterface>): BehaviorConfig {
      return BehaviorConfig.fromProps(Object.assign({}, this, props));
    }

    static fromProps(props: BehaviorConfigInterface): BehaviorConfig {
      return new BehaviorConfig(
        props.exportId,
        props.name,
        props.responseType,
        props.canBeMemoized,
        props.isDataType,
        props.isTest,
        props.dataTypeConfig
      );
    }

    static fromJson(props: BehaviorConfigJson): BehaviorConfig {
      const materializedProps = Object.assign({}, props, {
        canBeMemoized: Boolean(props.canBeMemoized),
        dataTypeConfig: props.dataTypeConfig ? DataTypeConfig.fromJson(props.dataTypeConfig) : null,
        responseType: BehaviorResponseType.fromProps(props.responseType)
      });
      return BehaviorConfig.fromProps(materializedProps);
    }

}

export default BehaviorConfig;

