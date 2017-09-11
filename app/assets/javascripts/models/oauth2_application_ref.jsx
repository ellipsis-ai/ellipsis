define(function() {
  class OAuth2ApplicationRef {
    constructor(props) {
      Object.defineProperties(this, {
        applicationId: { value: props.applicationId, enumerable: true },
        apiId: { value: props.apiId, enumerable: true },
        scope: { value: props.scope, enumerable: true },
        displayName: { value: props.displayName, enumerable: true },
        nameInCode: { value: props.nameInCode, enumerable: true }
      });
    }

    static fromJson(props) {
      return new OAuth2ApplicationRef(props);
    }
  }

  return OAuth2ApplicationRef;
});
