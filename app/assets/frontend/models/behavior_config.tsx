import DataTypeConfig, {DataTypeConfigJson} from './data_type_config';

export interface BehaviorConfigJson {
  exportId: string | null;
  name: string | null;
  forcePrivateResponse: boolean | null;
  isDataType: boolean;
  dataTypeConfig: DataTypeConfigJson | null;
}

interface BehaviorConfigInterface extends BehaviorConfigJson {
  dataTypeConfig: DataTypeConfig | null;
}

class BehaviorConfig implements BehaviorConfigInterface {
  constructor(
    readonly exportId: string | null,
    readonly name: string | null,
    readonly forcePrivateResponse: boolean | null,
    readonly isDataType: boolean,
    readonly dataTypeConfig: DataTypeConfig | null
  ) {
      Object.defineProperties(this, {
        exportId: { value: exportId, enumerable: true },
        name: { value: name, enumerable: true },
        forcePrivateResponse: { value: forcePrivateResponse, enumerable: true },
        isDataType: { value: isDataType, enumerable: true },
        dataTypeConfig: { value: dataTypeConfig, enumerable: true }
      });
    }

    getDataTypeConfig(): DataTypeConfig | null {
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
        props.isDataType,
        props.dataTypeConfig
      );
    }

    static fromJson(props: BehaviorConfigJson): BehaviorConfig {
      const materializedProps = Object.assign({}, props, props.dataTypeConfig ? {
        dataTypeConfig: DataTypeConfig.fromJson(props.dataTypeConfig)
      } : null);
      return BehaviorConfig.fromProps(materializedProps);
    }

}

export default BehaviorConfig;

