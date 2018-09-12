import Formatter from '../lib/formatter';
import RequiredApiConfig from "./required_api_config";

export interface ApiConfigRefJson {
  id: string;
  displayName: string;
}

abstract class ApiConfigRef implements ApiConfigRefJson {
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
}

export default ApiConfigRef;
