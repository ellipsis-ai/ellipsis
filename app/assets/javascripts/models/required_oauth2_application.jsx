define(function(require) {
  const OAuth2ApplicationRef = require('./oauth2_application_ref');
  class RequiredOAuth2Application {
    constructor(props) {
      Object.defineProperties(this, {
        id: { value: props.id, enumerable: true },
        apiId: { value: props.apiId, enumerable: true },
        recommendedScope: { value: props.recommendedScope, enumerable: true },
        nameInCode: { value: props.nameInCode, enumerable: true },
        application: { value: props.application, enumerable: true }
      });
    }

    apiLogoUrl() {
      return this.props.apiLogoUrl;
    }

    clone(props) {
      return new RequiredOAuth2Application((Object.assign({}, this, props)));
    }

    static fromJson(props) {
      return new RequiredOAuth2Application(Object.assign({}, props, {
        config: props.config ? OAuth2ApplicationRef.fromJson(props.config) : undefined
      }));
    }
  }

  return RequiredOAuth2Application;
});
