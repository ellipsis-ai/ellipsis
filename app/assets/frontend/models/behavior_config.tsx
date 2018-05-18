import DataTypeConfig, {DataTypeConfigJson} from './data_type_config';

export interface BehaviorConfigJson {
  exportId?: Option<string>;
  name?: Option<string>;
  forcePrivateResponse?: Option<boolean>;
  canBeMemoized?: Option<boolean>;
  isDataType: boolean;
  dataTypeConfig?: Option<DataTypeConfigJson>;
}

interface BehaviorConfigInterface extends BehaviorConfigJson {
  dataTypeConfig?: Option<DataTypeConfig>;
  forcePrivateResponse: boolean;
  canBeMemoized: boolean;
}

class BehaviorConfig implements BehaviorConfigInterface {
  constructor(
    readonly exportId: Option<string>,
    readonly name: Option<string>,
    readonly forcePrivateResponse: boolean,
    readonly canBeMemoized: boolean,
    readonly isDataType: boolean,
    readonly dataTypeConfig: Option<DataTypeConfig>
  ) {
      Object.defineProperties(this, {
        exportId: { value: exportId, enumerable: true },
        name: { value: name, enumerable: true },
        forcePrivateResponse: { value: Boolean(forcePrivateResponse), enumerable: true },
        canBeMemoized: { value: Boolean(canBeMemoized), enumerable: true },
        isDataType: { value: isDataType, enumerable: true },
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
        props.forcePrivateResponse,
        props.canBeMemoized,
        props.isDataType,
        props.dataTypeConfig
      );
    }

    static fromJson(props: BehaviorConfigJson): BehaviorConfig {
      const materializedProps = Object.assign({}, props, props.dataTypeConfig ? {
        dataTypeConfig: DataTypeConfig.fromJson(props.dataTypeConfig),
        forcePrivateResponse: Boolean(props.forcePrivateResponse),
        canBeMemoized: Boolean(props.canBeMemoized)
      } : null);
      return BehaviorConfig.fromProps(materializedProps);
    }

}

export default BehaviorConfig;

