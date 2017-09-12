define(function(require) {
  const ApiConfigRef = require('./api_config_ref');

  class OAuth2ApplicationRef extends ApiConfigRef {
    constructor(props) {
      super(props);
      Object.defineProperties(this, {
        apiId: { value: props.apiId, enumerable: true },
        scope: { value: props.scope, enumerable: true }
      });
    }

    static fromJson(props) {
      return new OAuth2ApplicationRef(props);
    }
  }

  return OAuth2ApplicationRef;
});
