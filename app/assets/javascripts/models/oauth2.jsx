define(function(require) {
  const ApiConfigRef = require('./api_config_ref');
  const RequiredApiConfig = require('./required_api_config');

  class OAuth2ApplicationRef extends ApiConfigRef {
    constructor(props) {
      super(props);
      Object.defineProperties(this, {
        apiId: { value: props.apiId, enumerable: true },
        scope: { value: props.scope, enumerable: true }
      });
    }

    newRequired() {
      return new RequiredOAuth2Application({
        apiId: this.apiId,
        recommendedScope: this.scope,
        nameInCode: this.nameInCode,
        application: this
      });
    }

    static fromJson(props) {
      return new OAuth2ApplicationRef(props);
    }
  }

  class RequiredOAuth2Application extends RequiredApiConfig {
    constructor(props) {
      super(props);
      Object.defineProperties(this, {
        id: { value: props.id, enumerable: true },
        apiId: { value: props.apiId, enumerable: true },
        recommendedScope: { value: props.recommendedScope, enumerable: true },
        nameInCode: { value: props.nameInCode, enumerable: true },
        application: { value: props.application, enumerable: true }
      });
    }

    onAddConfigFor(editor) {
      return editor.onAddOAuth2Application.bind(this);
    }

    onRemoveConfigFor(editor) {
      return editor.onRemoveOAuth2Application.bind(this);
    }

    getApiLogoUrl() {
      const api = undefined; //this.props.getOAuth2ApiWithId(apiId);
      return api ? (api.logoImageUrl || api.iconImageUrl) : undefined;
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

  return {
    'OAuth2ApplicationRef': OAuth2ApplicationRef,
    'RequiredOAuth2Application': RequiredOAuth2Application
  };
});
