define(function(require) {
  const ApiConfigRef = require('./api_config_ref');
  const RequiredApiConfig = require('./required_api_config');

  class RequiredOAuth2Application extends RequiredApiConfig {
    constructor(props) {
      super(props);
      Object.defineProperties(this, {
        recommendedScope: { value: props.recommendedScope, enumerable: true }
      });
    }

    onAddConfigFor(editor) {
      return editor.onAddOAuth2Application;
    }

    onRemoveConfigFor(editor) {
      return editor.onRemoveOAuth2Application;
    }

    onUpdateConfigFor(editor) {
      return editor.onUpdateOAuth2Application;
    }

    getApiLogoUrl() {
      const api = undefined; //this.props.getOAuth2ApiWithId(apiId);
      return api ? (api.logoImageUrl || api.iconImageUrl) : undefined;
    }

    getAllConfigsFrom(editor) {
      return editor.getAllOAuth2Applications().filter(ea => ea.apiId === this.apiId);
    }

    codePathPrefix() {
      return "ellipsis.accessTokens.";
    }

    codePath() {
      return `${this.codePathPrefix()}${this.nameInCode}`;
    }

    configString() {
      if (this.config) {
        return `using: ${this.config.displayName}`;
      } else {
        return "not yet configured";
      }
    }

    clone(props) {
      return new RequiredOAuth2Application((Object.assign({}, this, props)));
    }

  }

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
        config: this
      });
    }

    static fromJson(props) {
      return new OAuth2ApplicationRef(props);
    }
  }

  RequiredOAuth2Application.fromJson = function (props) {
    return new RequiredOAuth2Application(Object.assign({}, props, {
      config: props.config ? OAuth2ApplicationRef.fromJson(props.config) : undefined
    }));
  };

  return {
    'OAuth2ApplicationRef': OAuth2ApplicationRef,
    'RequiredOAuth2Application': RequiredOAuth2Application
  };

});
