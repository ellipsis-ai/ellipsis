// @flow
define(function(require) {
  const Formatter = require('../lib/formatter');

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

  return ApiConfigRef;
});
