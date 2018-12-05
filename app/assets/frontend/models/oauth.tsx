import {Diffable, DiffableProp} from "./diffs";

import ApiConfigRef, {ApiConfigRefJson, ApiJson} from './api_config_ref';
import RequiredApiConfigWithConfig from './required_api_config_with_config';
import ID from '../lib/id';
import {RequiredApiConfigJson} from "./required_api_config";
import {ApiConfigEditor} from "../behavior_editor/api_config_panel";
import BehaviorEditor from "../behavior_editor";

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

  constructor(
    readonly id: Option<string>,
    readonly exportId: Option<string>,
    readonly apiId: string,
    readonly nameInCode: string,
    readonly config: Option<OAuthApplicationRef>,
    readonly recommendedScope: string
  ) {
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

  clone(props: Partial<RequiredOAuthApplication>) {
    const oldOne: RequiredOAuthApplicationInterface = Object.assign({}, this);
    const newOne: RequiredOAuthApplicationInterface = Object.assign(oldOne, props);
    return RequiredOAuthApplication.fromProps(Object.assign({}, oldOne, newOne)) as this;
  }

  editorFor(editor: BehaviorEditor) {
    return RequiredOAuthApplication.editorFor(editor);
  }

  static editorFor(editor: BehaviorEditor): ApiConfigEditor<RequiredOAuthApplication> {
    return {
      allApiConfigsFor: editor.getAllOAuthApplications(),
      onGetApiLogoUrl: editor.getOAuthLogoUrlForConfig,
      onGetApiName: editor.getOAuthApiNameForConfig,
      onAddConfig: editor.onAddOAuthApplication,
      onAddNewConfig: editor.addNewOAuthApplication,
      onRemoveConfig: editor.onRemoveOAuthApplication,
      onUpdateConfig: editor.onUpdateOAuthApplication,
    }
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
  constructor(
    readonly id: string,
    readonly displayName: string,
    readonly apiId: string,
    readonly scope: string
  ) {
    super(id, displayName);
    Object.defineProperties(this, {
      apiId: { value: apiId, enumerable: true },
      scope: { value: scope, enumerable: true }
    });
  }

  getApiId(): string {
    return this.apiId;
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

  editorFor(editor: BehaviorEditor) {
    return RequiredOAuthApplication.editorFor(editor);
  }

  static fromJson(props: { id: string, displayName: string, apiId: string, scope: string }): OAuthApplicationRef {
    return new OAuthApplicationRef(props.id, props.displayName, props.apiId, props.scope);
  }
}

export interface OAuthApiJson extends ApiJson {
  apiId: string
  name: string
  requiresAuth: boolean
  newApplicationUrl?: Option<string>
  scopeDocumentationUrl?: Option<string>
  isOAuth1: boolean
}
