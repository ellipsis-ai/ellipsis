define(function(require) {
  const ApiConfigRef = require('./api_config_ref');
  const RequiredApiConfigWithConfig = require('./required_api_config_with_config');
  const ID = require('../lib/id');

  class RequiredOAuth2Application extends RequiredApiConfigWithConfig {
    constructor(props) {
      super(props);
      Object.defineProperties(this, {
        recommendedScope: { value: props.recommendedScope, enumerable: true }
      });
    }

    onAddConfigFor(editor) {
      return editor.onAddOAuth2Application;
    }

    onAddNewConfigFor(editor) {
      return editor.addNewOAuth2Application;
    }

    onRemoveConfigFor(editor) {
      return editor.onRemoveOAuth2Application;
    }

    onUpdateConfigFor(editor) {
      return editor.onUpdateOAuth2Application;
    }

    getApiLogoUrl(editor) {
      return editor.getOAuth2LogoUrlForConfig(this);
    }

    getApiName(editor) {
      return editor.getOAuth2ApiNameForConfig(this);
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
        return this.config.displayName;
      } else {
        return "Unconfigured";
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

    getApiLogoUrl(editor) {
      return editor.getOAuth2LogoUrlForConfig(this);
    }

    newRequired() {
      return new RequiredOAuth2Application({
        id: ID.next(),
        apiId: this.apiId,
        recommendedScope: this.scope,
        nameInCode: this.defaultNameInCode(),
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
