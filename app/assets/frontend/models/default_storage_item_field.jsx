// @flow
class DefaultStorageItemField {
    name: string;
    value: any;
    stringValue: string;

    constructor(name: string, value: any) {
      Object.defineProperties(this, {
        name: {
          value: name,
          enumerable: true
        },
        value: {
          value: value,
          enumerable: true
        },
        stringValue: {
          value: String(value),
          enumerable: true
        }
      });
    }
}

export default DefaultStorageItemField;

