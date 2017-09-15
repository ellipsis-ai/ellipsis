define(function(require) {
  const ApiConfigRef = require('./api_config_ref');
  const RequiredApiConfig = require('./required_api_config');
  const ID = require('../lib/id');

  class RequiredSimpleTokenApi extends RequiredApiConfig {

    onAddConfigFor(editor) {
      return editor.onAddSimpleTokenApi;
    }

    onRemoveConfigFor(editor) {
      return editor.onRemoveSimpleTokenApi;
    }

    onUpdateConfigFor(editor) {
      return editor.onUpdateSimpleTokenApi;
    }

    getApiLogoUrl(editor) {
      return editor.getSimpleTokenLogoUrl;
    }

    getAllConfigsFrom(editor) {
      return editor.getAllSimpleTokenApis().filter(ea => ea.id === this.apiId);
    }

    codePathPrefix() {
      return "ellipsis.accessTokens.";
    }

    codePath() {
      return `${this.codePathPrefix()}${this.nameInCode}`;
    }

    configString() {
      return "";
    }

    clone(props) {
      return new RequiredSimpleTokenApi((Object.assign({}, this, props)));
    }

  }

  class SimpleTokenApiRef extends ApiConfigRef {

    constructor(props) {
      super(props);
      Object.defineProperties(this, {
        logoImageUrl: { value: props.logoImageUrl, enumerable: true }
      });
    }

    newRequired() {
      return new RequiredSimpleTokenApi({
        id: ID.next(),
        apiId: this.id,
        nameInCode: this.defaultNameInCode(),
        config: this
      });
    }

    static fromJson(props) {
      return new SimpleTokenApiRef(props);
    }

  }

  RequiredSimpleTokenApi.fromJson = function (props) {
    return new RequiredSimpleTokenApi(Object.assign({}, props, {
      config: props.config ? SimpleTokenApiRef.fromJson(props.config) : undefined
    }));
  };

  return {
    'SimpleTokenApiRef': SimpleTokenApiRef,
    'RequiredSimpleTokenApi': RequiredSimpleTokenApi
  };

});
