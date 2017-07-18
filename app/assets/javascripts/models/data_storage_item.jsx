define(function(require) {
  const DataStorageItemField = require('./data_storage_item_field');

  class DataStorageItem {
    constructor(json) {
      Object.defineProperties(this, {
        id: {
          value: json ? json.id : null,
          enumerable: true
        },
        fields: {
          value: DataStorageItem.dataJsonToFields(json),
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
        .map((fieldName) => new DataStorageItemField(fieldName, json[fieldName]));
    }
  }

  return DataStorageItem;
});
