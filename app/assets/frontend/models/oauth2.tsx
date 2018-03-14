import {Diffable, DiffableProp} from "./diffs";

import ApiConfigRef, {ApiConfigRefJson} from './api_config_ref';
import RequiredApiConfigWithConfig from './required_api_config_with_config';
import ID from '../lib/id';

type callback = () => void

type RequiredOAuth2Editor = {
  onAddOAuth2Application: (r: RequiredOAuth2Application, c?: callback | null) => void,
  addNewOAuth2Application: (r?: RequiredOAuth2Application | null) => void,
  onRemoveOAuth2Application: (r: RequiredOAuth2Application, c?: callback | null) => void,
  onUpdateOAuth2Application: (r: RequiredOAuth2Application, c?: callback | null) => void,
  getOAuth2LogoUrlForConfig: (r: RequiredOAuth2Application) => string,
  getOAuth2ApiNameForConfig: (r: RequiredOAuth2Application) => string,
  getAllOAuth2Applications: () => Array<RequiredOAuth2Application>
}

type OAuth2ApplicationRefEditor = {
  getOAuth2LogoUrlForConfig: (OAuth2ApplicationRef) => string,
  getOAuth2ApiNameForConfig: (OAuth2ApplicationRef) => string,
}

export interface RequiredOAuth2ApplicationJson {
  id: string,
  exportId: string | null,
  apiId: string,
  nameInCode: string,
  config: OAuth2ApplicationRefJson | null,
  recommendedScope: string
}

interface RequiredOAuth2ApplicationInterface extends RequiredOAuth2ApplicationJson {
  config: OAuth2ApplicationRef | null
}

class RequiredOAuth2Application extends RequiredApiConfigWithConfig implements Diffable, RequiredOAuth2ApplicationInterface {
  readonly config: OAuth2ApplicationRef | null;
  readonly recommendedScope: string;

    constructor(id: string, exportId: string | null, apiId: string, nameInCode: string, config: OAuth2ApplicationRef | null, recommendedScope: string) {
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

    onAddConfigFor(editor: RequiredOAuth2Editor) {
      return editor.onAddOAuth2Application;
    }

    onAddNewConfigFor(editor: RequiredOAuth2Editor) {
      return editor.addNewOAuth2Application;
    }

    onRemoveConfigFor(editor: RequiredOAuth2Editor) {
      return editor.onRemoveOAuth2Application;
    }

    onUpdateConfigFor(editor: RequiredOAuth2Editor) {
      return editor.onUpdateOAuth2Application;
    }

    getApiLogoUrl(editor: RequiredOAuth2Editor) {
      return editor.getOAuth2LogoUrlForConfig(this);
    }

    getApiName(editor: RequiredOAuth2Editor) {
      return editor.getOAuth2ApiNameForConfig(this);
    }

    getAllConfigsFrom(editor: RequiredOAuth2Editor) {
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

    clone(props: Partial<RequiredOAuth2ApplicationInterface>): RequiredOAuth2Application {
      return RequiredOAuth2Application.fromProps(Object.assign({}, this, props));
    }

    static fromProps(props: RequiredOAuth2ApplicationInterface) {
      return new RequiredOAuth2Application(
        props.id,
        props.exportId,
        props.apiId,
        props.nameInCode,
        props.config,
        props.recommendedScope
      );
    }

    static fromJson(props: RequiredOAuth2ApplicationJson): RequiredOAuth2Application {
      const config = props.config ? OAuth2ApplicationRef.fromJson(props.config) : null;
      return new RequiredOAuth2Application(props.id, props.exportId, props.apiId, props.nameInCode, config, props.recommendedScope);
    }
}

  export interface OAuth2ApplicationRefJson extends ApiConfigRefJson {
    apiId: string;
    scope: string;
  }

  class OAuth2ApplicationRef extends ApiConfigRef implements OAuth2ApplicationRefJson {
    readonly apiId: string;
    readonly scope: string;

    constructor(id: string, displayName: string, apiId: string, scope: string) {
      super(id, displayName);
      Object.defineProperties(this, {
        apiId: { value: apiId, enumerable: true },
        scope: { value: scope, enumerable: true }
      });
    }

    getApiLogoUrl(editor: OAuth2ApplicationRefEditor) {
      return editor.getOAuth2LogoUrlForConfig(this);
    }

    getApiName(editor: OAuth2ApplicationRefEditor) {
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

    static fromJson(props: { id: string, displayName: string, apiId: string, scope: string }): OAuth2ApplicationRef {
      return new OAuth2ApplicationRef(props.id, props.displayName, props.apiId, props.scope);
    }
}

export {OAuth2ApplicationRef, RequiredOAuth2Application};
