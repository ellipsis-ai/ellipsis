import Formatter from '../lib/formatter';

export interface ApiConfigRefJson {
  id: string;
  displayName: string;
}

class ApiConfigRef implements ApiConfigRefJson {
    id: string;
    displayName: string;

    constructor(id: string, displayName: string) {
      Object.defineProperties(this, {
        id: { value: id, enumerable: true },
        displayName: { value: displayName, enumerable: true }
      });
    }

    defaultNameInCode(): string {
      return Formatter.formatCamelCaseIdentifier(this.displayName);
    }
}

export default ApiConfigRef;
