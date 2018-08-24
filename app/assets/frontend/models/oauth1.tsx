import {Diffable, DiffableProp} from "./diffs";

import ApiConfigRef, {ApiConfigRefJson} from './api_config_ref';
import RequiredApiConfigWithConfig from './required_api_config_with_config';
import ID from '../lib/id';
import {RequiredApiConfigJson} from "./required_api_config";

type callback = () => void

type RequiredOAuth1Editor = {
  onAddOAuth1Application: (r: RequiredOAuth1Application, c?: Option<callback>) => void,
  addNewOAuth1Application: (r?: Option<RequiredOAuth1Application>) => void,
  onRemoveOAuth1Application: (r: RequiredOAuth1Application, c?: Option<callback>) => void,
  onUpdateOAuth1Application: (r: RequiredOAuth1Application, c?: Option<callback>) => void,
  getOAuth1LogoUrlForConfig: (r: RequiredOAuth1Application) => string,
  getOAuth1ApiNameForConfig: (r: RequiredOAuth1Application) => string,
  getAllOAuth1Applications: () => Array<RequiredOAuth1Application>
}

type OAuth1ApplicationRefEditor = {
  getOAuth1LogoUrlForConfig: (OAuth1ApplicationRef) => string,
  getOAuth1ApiNameForConfig: (OAuth1ApplicationRef) => string,
}

export interface RequiredOAuth1ApplicationJson extends RequiredApiConfigJson {
  config?: Option<OAuth1ApplicationRefJson>,
  recommendedScope: string
}

interface RequiredOAuth1ApplicationInterface extends RequiredOAuth1ApplicationJson {
  config?: Option<OAuth1ApplicationRef>
}

class RequiredOAuth1Application extends RequiredApiConfigWithConfig implements Diffable, RequiredOAuth1ApplicationInterface {
  readonly config: Option<OAuth1ApplicationRef>;
  readonly recommendedScope: string;

  constructor(id: Option<string>, exportId: Option<string>, apiId: string, nameInCode: string, config: Option<OAuth1ApplicationRef>, recommendedScope: string) {
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
    }];
  }

  onAddConfigFor(editor: RequiredOAuth1Editor) {
    return editor.onAddOAuth1Application;
  }

  onAddNewConfigFor(editor: RequiredOAuth1Editor) {
    return editor.addNewOAuth1Application;
  }

  onRemoveConfigFor(editor: RequiredOAuth1Editor) {
    return editor.onRemoveOAuth1Application;
  }

  onUpdateConfigFor(editor: RequiredOAuth1Editor) {
    return editor.onUpdateOAuth1Application;
  }

  getApiLogoUrl(editor: RequiredOAuth1Editor) {
    return editor.getOAuth1LogoUrlForConfig(this);
  }

  getApiName(editor: RequiredOAuth1Editor) {
    return editor.getOAuth1ApiNameForConfig(this);
  }

  getAllConfigsFrom(editor: RequiredOAuth1Editor) {
    return editor.getAllOAuth1Applications().filter(ea => ea.apiId === this.apiId);
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

  clone(props: Partial<RequiredOAuth1ApplicationInterface>): RequiredOAuth1Application {
    return RequiredOAuth1Application.fromProps(Object.assign({}, this, props));
  }

  static fromProps(props: RequiredOAuth1ApplicationInterface) {
    return new RequiredOAuth1Application(
      props.id,
      props.exportId,
      props.apiId,
      props.nameInCode,
      props.config,
      props.recommendedScope
    );
  }

  static fromJson(props: RequiredOAuth1ApplicationJson): RequiredOAuth1Application {
    const config = props.config ? OAuth1ApplicationRef.fromJson(props.config) : null;
    return new RequiredOAuth1Application(props.id, props.exportId, props.apiId, props.nameInCode, config, props.recommendedScope);
  }
}

export interface OAuth1ApplicationRefJson extends ApiConfigRefJson {
  apiId: string;
  scope: string;
}

class OAuth1ApplicationRef extends ApiConfigRef implements OAuth1ApplicationRefJson {
  readonly apiId: string;
  readonly scope: string;

  constructor(id: string, displayName: string, apiId: string, scope: string) {
    super(id, displayName);
    Object.defineProperties(this, {
      apiId: { value: apiId, enumerable: true },
      scope: { value: scope, enumerable: true }
    });
  }

  getApiLogoUrl(editor: OAuth1ApplicationRefEditor) {
    return editor.getOAuth1LogoUrlForConfig(this);
  }

  getApiName(editor: OAuth1ApplicationRefEditor) {
    return editor.getOAuth1ApiNameForConfig(this);
  }

  configName() {
    return this.displayName;
  }

  newRequired() {
    return new RequiredOAuth1Application(
      ID.next(),
      ID.next(),
      this.apiId,
      this.defaultNameInCode(),
      this,
      this.scope
    );
  }

  static fromJson(props: { id: string, displayName: string, apiId: string, scope: string }): OAuth1ApplicationRef {
    return new OAuth1ApplicationRef(props.id, props.displayName, props.apiId, props.scope);
  }
}

export {OAuth1ApplicationRef, RequiredOAuth1Application};
