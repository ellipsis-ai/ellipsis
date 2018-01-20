// @flow
define(function(require) {
  const DefaultStorageItemField = require('./default_storage_item_field');

  type DefaultStorageItemData = { [string]: any };

  class DefaultStorageItem {
    id: string;
    behaviorId: string;
    updatedAt: ?number;
    updatedByUserId: ?string;
    data: DefaultStorageItemData;
    fields: Array<DefaultStorageItemField>;

    constructor(
      id: string,
      behaviorId: string,
      updatedAt: ?number,
      updatedByUserId: ?string,
      data: DefaultStorageItemData
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

  return DefaultStorageItem;
});
