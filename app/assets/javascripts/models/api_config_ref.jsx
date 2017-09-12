define(function() {
  class ApiConfigRef {
    constructor(props) {
      Object.defineProperties(this, {
        id: { value: props.id, enumerable: true },
        displayName: { value: props.displayName, enumerable: true },
        nameInCode: { value: props.nameInCode, enumerable: true }
      });
    }

    getSelectorLabel() {
      return this.displayName || "meh";
    }
  }

  return ApiConfigRef;
});
