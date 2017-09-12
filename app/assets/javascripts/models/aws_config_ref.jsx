define(function(require) {
  const ApiConfigRef = require('./api_config_ref');

  class AWSConfigRef extends ApiConfigRef {
    constructor(props) {
      super(props);
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
