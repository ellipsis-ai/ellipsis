// @flow

import type {Diff, Diffable} from "./diffs";

define(function(require) {
  const ApiConfigRef = require('./api_config_ref');
  const diffs = require('./diffs');
  const RequiredApiConfig = require('./required_api_config');
  const ID = require('../lib/id');

  class RequiredSimpleTokenApi extends RequiredApiConfig implements Diffable {

    maybeDiffFor(other: RequiredSimpleTokenApi): ?diffs.ModifiedDiff<RequiredSimpleTokenApi> {
      const children: Array<Diff> = [
        this.maybeNameInCodeDiffFor(other)
      ].filter(ea => Boolean(ea));
      if (children.length === 0) {
        return null;
      } else {
        return new diffs.ModifiedDiff(children, this, other);
      }
    }

    onAddConfigFor(editor) {
      return editor.onAddSimpleTokenApi;
    }

    onAddNewConfigFor() {
      return undefined; // N/A
    }

    onRemoveConfigFor(editor) {
      return editor.onRemoveSimpleTokenApi;
    }

    onUpdateConfigFor(editor) {
      return editor.onUpdateSimpleTokenApi;
    }

    getApiLogoUrl(editor) {
      return editor.getSimpleTokenLogoUrlForConfig(this);
    }

    getApiName(editor) {
      return editor.getSimpleTokenNameForConfig(this);
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

    configName() {
      return "";
    }

    isConfigured() {
      return true;
    }

    clone(props): RequiredSimpleTokenApi {
      return RequiredSimpleTokenApi.fromProps(Object.assign({}, this, props));
    }

    static fromProps(props): RequiredSimpleTokenApi {
      return new RequiredSimpleTokenApi(props.id, props.requiredId, props.apiId, props.nameInCode, props.config);
    }

  }

  class SimpleTokenApiRef extends ApiConfigRef {
    logoImageUrl: string;

    constructor(id: string, displayName: string, logoImageUrl: string) {
      super(id, displayName);
      Object.defineProperties(this, {
        logoImageUrl: { value: logoImageUrl, enumerable: true }
      });
    }

    getApiLogoUrl() {
      return this.logoImageUrl;
    }

    getApiName() {
      return this.displayName;
    }

    configName() {
      return this.displayName;
    }

    newRequired() {
      return new RequiredSimpleTokenApi(
        ID.next(),
        ID.next(),
        this.id,
        this.defaultNameInCode(),
        this
      );
    }

    static fromJson(props) {
      return new SimpleTokenApiRef(props.id, props.displayName, props.logoImageUrl);
    }

  }

  RequiredSimpleTokenApi.fromJson = function (props) {
    return RequiredSimpleTokenApi.fromProps(Object.assign({}, props, {
      config: props.config ? SimpleTokenApiRef.fromJson(props.config) : undefined
    }));
  };

  return {
    'SimpleTokenApiRef': SimpleTokenApiRef,
    'RequiredSimpleTokenApi': RequiredSimpleTokenApi
  };

});
