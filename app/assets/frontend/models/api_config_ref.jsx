// @flow
import Formatter from '../lib/formatter';

class ApiConfigRef {
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
