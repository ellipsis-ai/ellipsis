import Formatter from '../lib/formatter';
import RequiredApiConfig, {RequiredApiConfigEditor} from "./required_api_config";

export interface ApiConfigRefJson {
  id: string;
  displayName: string;
  logoImageUrl?: Option<string>;
  iconImageUrl?: Option<string>;
}

abstract class ApiConfigRef implements ApiConfigRefJson {
  readonly logoImageUrl: Option<string>;
  readonly iconImageUrl: Option<string>;

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

  abstract getApiName(editor: RequiredApiConfigEditor): string
  abstract getApiLogoUrl(editor: RequiredApiConfigEditor): string

  abstract configName(): string
}

export default ApiConfigRef;
