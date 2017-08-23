define(function() {
  class AWSConfigRef {
    constructor(props) {
      Object.defineProperties(this, {
        id: { value: props.id, enumerable: true },
        displayName: { value: props.displayName, enumerable: true },
        nameInCode: { value: props.nameInCode, enumerable: true }
      });
    }

    static fromJson(props) {
      return new AWSConfigRef(props);
    }
  }

  return AWSConfigRef;
});
