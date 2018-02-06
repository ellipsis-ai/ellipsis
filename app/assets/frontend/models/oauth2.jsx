// @flow
import type {Diffable, DiffableProp} from "./diffs";

import ApiConfigRef from './api_config_ref';
import RequiredApiConfigWithConfig from './required_api_config_with_config';
import ID from '../lib/id';

class RequiredOAuth2Application extends RequiredApiConfigWithConfig implements Diffable {
    recommendedScope: string;

    constructor(id: string, exportId: string, apiId: string, nameInCode: string, config: ApiConfigRef, recommendedScope: string) {
      super(id, exportId, apiId, nameInCode, config);
      Object.defineProperties(this, {
        recommendedScope: { value: recommendedScope, enumerable: true }
      });
    }

    diffProps(): Array<DiffableProp> {
      return [{
        name: "Name used in code",
        value: this.nameInCode || ""
      }, {
        name: "Configuration to use",
        value: this.configName()
      }, {
        name: "Recommended scope",
        value: this.recommendedScope
      }];
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

    configName() {
      return this.config ? this.config.displayName : "";
    }

    isConfigured() {
      return Boolean(this.config);
    }

    clone(props) {
      return RequiredOAuth2Application.fromProps(Object.assign({}, this, props));
    }

    static fromProps(props) {
      return new RequiredOAuth2Application(
        props.id,
        props.exportId,
        props.apiId,
        props.nameInCode,
        props.config,
        props.recommendedScope
      );
    }

  }

  class OAuth2ApplicationRef extends ApiConfigRef {
    apiId: string;
    scope: string;

    constructor(id: string, displayName: string, apiId: string, scope: string) {
      super(id, displayName);
      Object.defineProperties(this, {
        apiId: { value: apiId, enumerable: true },
        scope: { value: scope, enumerable: true }
      });
    }

    getApiLogoUrl(editor) {
      return editor.getOAuth2LogoUrlForConfig(this);
    }

    getApiName(editor) {
      return editor.getOAuth2ApiNameForConfig(this);
    }

    configName() {
      return this.displayName;
    }

    newRequired() {
      return new RequiredOAuth2Application(
        ID.next(),
        ID.next(),
        this.apiId,
        this.defaultNameInCode(),
        this,
        this.scope
      );
    }

    static fromJson(props): OAuth2ApplicationRef {
      return new OAuth2ApplicationRef(props.id, props.displayName, props.apiId, props.scope);
    }
}

RequiredOAuth2Application.fromJson = function (props) {
  const config = props.config ? OAuth2ApplicationRef.fromJson(props.config) : undefined;
  return new RequiredOAuth2Application(props.id, props.exportId, props.apiId, props.nameInCode, config, props.scope);
};

export {OAuth2ApplicationRef, RequiredOAuth2Application};
