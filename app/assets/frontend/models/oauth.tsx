import {Diffable, DiffableProp} from "./diffs";

import ApiConfigRef, {ApiConfigRefJson} from './api_config_ref';
import RequiredApiConfigWithConfig from './required_api_config_with_config';
import ID from '../lib/id';
import RequiredApiConfig, {
  RequiredApiConfigJson,
  RequiredApiConfigEditor
} from "./required_api_config";

export interface RequiredOAuthEditor extends RequiredApiConfigEditor {
  onAddOAuthApplication: (r: RequiredApiConfig, c?: () => void) => void,
  addNewOAuthApplication: (r?: Option<RequiredApiConfig>) => void,
  onRemoveOAuthApplication: (r: RequiredApiConfig, c?: () => void) => void,
  onUpdateOAuthApplication: (r: RequiredApiConfig, c?: () => void) => void,
  getOAuthLogoUrlForConfig: (r: RequiredApiConfig) => string,
  getOAuthApiNameForConfig: (r: RequiredApiConfig) => string,
  getAllOAuthApplications: () => Array<OAuthApplicationRef>
}

type OAuthApplicationRefEditor = {
  getOAuthLogoUrlForConfig: (OAuthApplicationRef) => string,
  getOAuthApiNameForConfig: (OAuthApplicationRef) => string,
}

export interface RequiredOAuthApplicationJson extends RequiredApiConfigJson {
  config?: Option<OAuthApplicationRefJson>,
  recommendedScope: string
}

interface RequiredOAuthApplicationInterface extends RequiredOAuthApplicationJson {
  config?: Option<OAuthApplicationRef>
}

export class RequiredOAuthApplication
  extends RequiredApiConfigWithConfig
  implements Diffable, RequiredOAuthApplicationInterface {

  readonly config: Option<OAuthApplicationRef>;
  readonly recommendedScope: string;

  constructor(id: Option<string>, exportId: Option<string>, apiId: string, nameInCode: string, config: Option<OAuthApplicationRef>, recommendedScope: string) {
    super(id, exportId, apiId, nameInCode, config);
    Object.defineProperties(this, {
      recommendedScope: { value: recommendedScope, enumerable: true }
    });
  }

  diffProps(): Array<DiffableProp> {
    return [{
      name: "Name used in code",
      value: this.nameInCode || "",
      ignoreForPublished: true
    }, {
      name: "Configuration to use",
      value: this.configName(),
      ignoreForPublished: true
    }, {
      name: "Recommended scope",
      value: this.recommendedScope
    }];
  }

  onAddConfigFor(editor: RequiredOAuthEditor) {
    return editor.onAddOAuthApplication;
  }

  onAddNewConfigFor(editor: RequiredOAuthEditor) {
    return editor.addNewOAuthApplication;
  }

  onRemoveConfigFor(editor: RequiredOAuthEditor) {
    return editor.onRemoveOAuthApplication;
  }

  onUpdateConfigFor(editor: RequiredOAuthEditor) {
    return editor.onUpdateOAuthApplication;
  }

  getApiLogoUrl(editor: RequiredOAuthEditor) {
    return editor.getOAuthLogoUrlForConfig(this);
  }

  getApiName(editor: RequiredOAuthEditor) {
    return editor.getOAuthApiNameForConfig(this);
  }

  getAllConfigsFrom(editor: RequiredOAuthEditor): Array<OAuthApplicationRef> {
    return editor.getAllOAuthApplications().filter(ea => ea.apiId === this.apiId);
  }

  codePathPrefix() {
    return "ellipsis.accessTokens.";
  }

  codePath() {
    return `${this.codePathPrefix()}${this.nameInCode}`;
  }

  configName(): string {
    return this.config ? this.config.displayName : "";
  }

  isConfigured() {
    return Boolean(this.config);
  }

  clone(props: Partial<RequiredOAuthApplicationInterface>): RequiredOAuthApplication {
    return RequiredOAuthApplication.fromProps(Object.assign({}, this, props));
  }

  static fromProps(props: RequiredOAuthApplicationInterface) {
    return new RequiredOAuthApplication(
      props.id,
      props.exportId,
      props.apiId,
      props.nameInCode,
      props.config,
      props.recommendedScope
    );
  }

  static fromJson(props: RequiredOAuthApplicationJson): RequiredOAuthApplication {
    const config = props.config ? OAuthApplicationRef.fromJson(props.config) : null;
    return new RequiredOAuthApplication(props.id, props.exportId, props.apiId, props.nameInCode, config, props.recommendedScope);
  }
}

export interface OAuthApplicationRefJson extends ApiConfigRefJson {
  apiId: string;
  scope: string;
}

export class OAuthApplicationRef extends ApiConfigRef implements OAuthApplicationRefJson {
  readonly apiId: string;
  readonly scope: string;

  constructor(id: string, displayName: string, apiId: string, scope: string) {
    super(id, displayName);
    Object.defineProperties(this, {
      apiId: { value: apiId, enumerable: true },
      scope: { value: scope, enumerable: true }
    });
  }

  getApiLogoUrl(editor: OAuthApplicationRefEditor): string {
    return editor.getOAuthLogoUrlForConfig(this);
  }

  getApiName(editor: OAuthApplicationRefEditor) {
    return editor.getOAuthApiNameForConfig(this);
  }

  configName() {
    return this.displayName;
  }

  newRequired() {
    return new RequiredOAuthApplication(
      ID.next(),
      ID.next(),
      this.apiId,
      this.defaultNameInCode(),
      this,
      this.scope
    );
  }

  static fromJson(props: { id: string, displayName: string, apiId: string, scope: string }): OAuthApplicationRef {
    return new OAuthApplicationRef(props.id, props.displayName, props.apiId, props.scope);
  }
}

export interface OAuthApiJson {
  apiId: string
  name: string
  requiresAuth: boolean
  newApplicationUrl?: Option<string>
  scopeDocumentationUrl?: Option<string>
  iconImageUrl?: Option<string>
  logoImageUrl?: Option<string>
  isOAuth1: boolean
}
