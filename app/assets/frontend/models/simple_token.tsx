import {Diffable, DiffableProp} from "./diffs";

import ApiConfigRef from './api_config_ref';
import RequiredApiConfig from './required_api_config';
import ID from '../lib/id';

type callback = () => void

type SimpleTokenEditor = {
  onAddSimpleTokenApi: (r: RequiredSimpleTokenApi) => void,
  onRemoveSimpleTokenApi: (r: RequiredSimpleTokenApi) => void,
  onUpdateSimpleTokenApi: (r: RequiredSimpleTokenApi, c?: callback | null) => void,
  getSimpleTokenLogoUrlForConfig: (r: RequiredSimpleTokenApi) => string,
  getSimpleTokenNameForConfig: (r: RequiredSimpleTokenApi) => string,
  getAllSimpleTokenApis: () => Array<RequiredSimpleTokenApi>
}

class RequiredSimpleTokenApi extends RequiredApiConfig implements Diffable {

    static fromJson: (s: { config: SimpleTokenApiRef }) => RequiredSimpleTokenApi;

    diffProps(): Array<DiffableProp> {
      return [{
        name: "Name used in code",
        value: this.nameInCode || ""
      }];
    }

    onAddConfigFor(editor: SimpleTokenEditor) {
      return editor.onAddSimpleTokenApi;
    }

    onAddNewConfigFor() {
      return undefined; // N/A
    }

    onRemoveConfigFor(editor: SimpleTokenEditor) {
      return editor.onRemoveSimpleTokenApi;
    }

    onUpdateConfigFor(editor: SimpleTokenEditor) {
      return editor.onUpdateSimpleTokenApi;
    }

    getApiLogoUrl(editor: SimpleTokenEditor) {
      return editor.getSimpleTokenLogoUrlForConfig(this);
    }

    getApiName(editor: SimpleTokenEditor) {
      return editor.getSimpleTokenNameForConfig(this);
    }

    getAllConfigsFrom(editor: SimpleTokenEditor) {
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

    clone(props: {}): RequiredSimpleTokenApi {
      return RequiredSimpleTokenApi.fromProps(Object.assign({}, this, props));
    }

    static fromProps(props): RequiredSimpleTokenApi {
      return new RequiredSimpleTokenApi(props.id, props.exportId, props.apiId, props.nameInCode);
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
        this.defaultNameInCode()
      );
    }

    static fromJson(props: {} & SimpleTokenApiRef) {
      return new SimpleTokenApiRef(props.id, props.displayName, props.logoImageUrl);
    }

}

RequiredSimpleTokenApi.fromJson = function(props) {
  return RequiredSimpleTokenApi.fromProps(Object.assign({}, props, {
    config: props.config ? SimpleTokenApiRef.fromJson(props.config) : undefined
  }));
};

export {SimpleTokenApiRef, RequiredSimpleTokenApi};
