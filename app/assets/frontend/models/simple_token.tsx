import {Diffable, DiffableProp} from "./diffs";

import ApiConfigRef, {ApiConfigRefJson} from './api_config_ref';
import RequiredApiConfig, {
  RequiredApiConfigEditor,
  RequiredApiConfigJson
} from './required_api_config';
import ID from '../lib/id';

interface SimpleTokenEditor extends RequiredApiConfigEditor {
  onAddSimpleTokenApi: (r: RequiredApiConfig) => void,
  onRemoveSimpleTokenApi: (r: RequiredApiConfig) => void,
  onUpdateSimpleTokenApi: (r: RequiredApiConfig, c?: () => void) => void,
  getSimpleTokenLogoUrlForConfig: (r: RequiredApiConfig) => string,
  getSimpleTokenNameForConfig: (r: RequiredApiConfig) => string,
  getAllSimpleTokenApis: () => Array<SimpleTokenApiRef>
}

export interface RequiredSimpleTokenApiJson extends RequiredApiConfigJson {}

class RequiredSimpleTokenApi
  extends RequiredApiConfig
  implements Diffable, RequiredSimpleTokenApiJson {

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
      return () => {}; // N/A
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

    getAllConfigsFrom(editor: SimpleTokenEditor): Array<SimpleTokenApiRef> {
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

    static fromJson(props: RequiredSimpleTokenApiJson): RequiredSimpleTokenApi {
      return RequiredSimpleTokenApi.fromProps(props);
    }
  }

  export interface SimpleTokenApiRefJson extends ApiConfigRefJson {
    logoImageUrl: string
  }

  class SimpleTokenApiRef extends ApiConfigRef implements SimpleTokenApiRefJson {
    constructor(
      readonly id: string,
      readonly displayName: string,
      readonly logoImageUrl: string
    ) {
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

    static fromJson(props: SimpleTokenApiRefJson) {
      return new SimpleTokenApiRef(props.id, props.displayName, props.logoImageUrl);
    }

}

export {SimpleTokenApiRef, RequiredSimpleTokenApi};
