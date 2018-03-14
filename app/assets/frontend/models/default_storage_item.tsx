import DefaultStorageItemField from './default_storage_item_field';

interface DefaultStorageItemData {
  id: string,
  [prop: string]: any
}

export interface DefaultStorageItemJson {
  id: string,
  behaviorId: string,
  updatedAt: number | null,
  updatedByUserId: string | null,
  data: DefaultStorageItemData
}

class DefaultStorageItem implements DefaultStorageItemJson {
  readonly fields: Array<DefaultStorageItemField>;
  constructor(
    readonly id: string,
    readonly behaviorId: string,
    readonly updatedAt: number | null,
    readonly updatedByUserId: string | null,
    readonly data: DefaultStorageItemData
  ) {
      Object.defineProperties(this, {
        id: {
          value: id,
          enumerable: true
        },
        behaviorId: {
          value: behaviorId,
          enumerable: true
        },
        updatedAt: {
          value: updatedAt,
          enumerable: true
        },
        updatedByUserId: {
          value: updatedByUserId,
          enumerable: true
        },
        data: {
          value: data,
          enumerable: true
        },
        fields: {
          value: DefaultStorageItem.dataJsonToFields(data),
          enumerable: true
        }
      });
    }

    static dataJsonToFields(json: DefaultStorageItemData): Array<DefaultStorageItemField> {
      if (!json) {
        return [];
      }
      const fieldNames = Object.keys(json);
      return fieldNames
        .filter((ea) => ea === "id")
        .concat(fieldNames.filter((ea) => ea !== "id"))
        .map((fieldName) => new DefaultStorageItemField(fieldName, json[fieldName]));
    }
}

export default DefaultStorageItem;

