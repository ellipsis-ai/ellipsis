import Formatter from '../lib/formatter';
import RequiredApiConfig from "./required_api_config";
import BehaviorEditor from "../behavior_editor";
import {ApiConfigEditor} from "../behavior_editor/api_config_panel";

export interface ApiJson {
  logoImageUrl?: Option<string>;
  iconImageUrl?: Option<string>;
}

export interface ApiConfigRefJson extends ApiJson {
  id: string;
  displayName: string;
}

abstract class ApiConfigRef implements ApiConfigRefJson {
  readonly logoImageUrl: Option<string>;
  readonly iconImageUrl: Option<string>;
  apiId: string;

  constructor(
    readonly id: string,
    readonly displayName: string
  ) {
      Object.defineProperties(this, {
        id: { value: id, enumerable: true },
        displayName: { value: displayName, enumerable: true }
      });
  }

  defaultNameInCode(): string {
    return Formatter.formatCamelCaseIdentifier(this.displayName);
  }

  abstract newRequired(): RequiredApiConfig

  abstract configName(): string

  abstract editorFor(editor: BehaviorEditor): ApiConfigEditor<any>
}

export default ApiConfigRef;
