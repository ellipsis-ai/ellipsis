define(function(require) {
  const DefaultStorageItemField = require('./default_storage_item_field');

  class DefaultStorageItem {
    constructor(props) {
      const initialProps = Object.assign({}, props);
      Object.defineProperties(this, {
        id: {
          value: initialProps.id || null,
          enumerable: true
        },
        behaviorId: {
          value: initialProps.behaviorId || null,
          enumerable: true
        },
        updatedAt: {
          value: initialProps.updatedAt || null,
          enumerable: true
        },
        updatedByUserId: {
          value: initialProps.updatedByUserId || null,
          enumerable: true
        },
        data: {
          value: Object.assign({}, initialProps.data),
          enumerable: true
        },
        fields: {
          value: DefaultStorageItem.dataJsonToFields(initialProps.data),
          enumerable: true
        }
      });
    }

    static dataJsonToFields(json) {
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
