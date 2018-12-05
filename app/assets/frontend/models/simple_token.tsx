import {Diffable, DiffableProp} from "./diffs";

import ApiConfigRef, {ApiConfigRefJson} from './api_config_ref';
import RequiredApiConfig, {
  RequiredApiConfigEditor,
  RequiredApiConfigJson
} from './required_api_config';
import ID from '../lib/id';
import BehaviorEditor from "../behavior_editor";
import {ApiConfigEditor} from "../behavior_editor/api_config_panel";

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
    constructor(readonly id: Option<string>,
                readonly exportId: Option<string>,
                readonly apiId: string,
                readonly nameInCode: string) {
      super(id, exportId, apiId, nameInCode);
    }

    diffProps(): Array<DiffableProp> {
      return [{
        name: "Name used in code",
        value: this.nameInCode || ""
      }];
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

    clone(props: Partial<RequiredSimpleTokenApi>) {
      return RequiredSimpleTokenApi.fromProps(Object.assign({}, this, props)) as this;
    }

    static fromProps(props): RequiredSimpleTokenApi {
      return new RequiredSimpleTokenApi(props.id, props.exportId, props.apiId, props.nameInCode);
    }

    static fromJson(props: RequiredSimpleTokenApiJson): RequiredSimpleTokenApi {
      return RequiredSimpleTokenApi.fromProps(props);
    }

    editorFor(editor: BehaviorEditor): ApiConfigEditor<RequiredSimpleTokenApi> {
      return RequiredSimpleTokenApi.editorFor(editor);
    }

    static editorFor(editor: BehaviorEditor): ApiConfigEditor<RequiredSimpleTokenApi> {
      return {
        allApiConfigsFor: editor.getAllSimpleTokenApis(),
        onGetApiLogoUrl: editor.getSimpleTokenLogoUrlForConfig,
        onGetApiName: editor.getSimpleTokenNameForConfig,
        onAddConfig: editor.onAddSimpleTokenApi,
        onAddNewConfig: () => {},
        onRemoveConfig: editor.onRemoveSimpleTokenApi,
        onUpdateConfig: editor.onUpdateSimpleTokenApi
      };
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
        apiId: { value: id, enumerable: true },
        logoImageUrl: { value: logoImageUrl, enumerable: true }
      });
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

    editorFor(editor: BehaviorEditor): ApiConfigEditor<RequiredSimpleTokenApi> {
      return RequiredSimpleTokenApi.editorFor(editor);
    }

    static fromJson(props: SimpleTokenApiRefJson) {
      return new SimpleTokenApiRef(props.id, props.displayName, props.logoImageUrl);
    }

}

export {SimpleTokenApiRef, RequiredSimpleTokenApi};
