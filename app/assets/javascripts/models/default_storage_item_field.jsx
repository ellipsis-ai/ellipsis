define(function() {
  class DefaultStorageItemField {
    constructor(name, value) {
      Object.defineProperties(this, {
        name: {
          value: name,
          enumerable: true
        },
        value: {
          value: value,
          enumerable: true
        }
      });
    }
  }

  return DefaultStorageItemField;
});
