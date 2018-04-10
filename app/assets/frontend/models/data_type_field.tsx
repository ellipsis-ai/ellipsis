import ParamType, {ParamTypeJson} from './param_type';

export interface DataTypeFieldJson {
  fieldId: string;
  fieldVersionId?: Option<string>;
  name: string;
  fieldType: ParamTypeJson;
  isLabel?: boolean;
}

interface DataTypeFieldInterface extends DataTypeFieldJson {
  fieldType: ParamType;
}

class DataTypeField implements DataTypeFieldInterface {
  readonly name: string;
  constructor(
    readonly fieldId: string,
    readonly fieldVersionId: Option<string>,
    name: Option<string>,
    readonly fieldType: ParamType,
    isLabel: Option<boolean>
  ) {
      Object.defineProperties(this, {
        fieldVersionId: {
          value: fieldVersionId,
          emumerable: true
        },
        fieldId: {
          value: fieldId,
          enumerable: true
        },
        name: {
          value: name || "",
          enumerable: true
        },
        fieldType: {
          value: fieldType,
          enumerable: true
        },
        isLabel: {
          value: !!isLabel,
          enumerable: true
        }
      });
  }

    clone(props: Partial<DataTypeFieldInterface>): DataTypeField {
      const newProps = Object.assign({}, this, props);
      return DataTypeField.fromProps(newProps);
    }

    static fromProps(props: DataTypeFieldInterface): DataTypeField {
      return new DataTypeField(
        props.fieldId,
        props.fieldVersionId ? props.fieldVersionId : null,
        props.name,
        props.fieldType,
        props.isLabel ? props.isLabel : false
      );
    }

    static fromJson(props: DataTypeFieldJson): DataTypeField {
      const materializedProps = Object.assign({}, props, props.fieldType ? {
        fieldType: ParamType.fromJson(props.fieldType)
      } : null);
      return DataTypeField.fromProps(materializedProps);
    }

    static fieldsFromJson(jsonArray: Array<DataTypeFieldJson>): Array<DataTypeField> {
      return jsonArray.map(DataTypeField.fromJson);
    }

  }

export default DataTypeField;

