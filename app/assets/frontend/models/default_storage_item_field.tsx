class DefaultStorageItemField {
  readonly stringValue: string;

  constructor(
    readonly name: string,
    readonly value: any
  ) {
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

