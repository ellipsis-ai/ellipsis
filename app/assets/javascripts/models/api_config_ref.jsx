// @flow
define(function(require) {
  const Formatter = require('../lib/formatter');

  class ApiConfigRef {
    id: string;
    displayName: string;

    constructor(props) {
      Object.defineProperties(this, {
        id: { value: props.id, enumerable: true },
        displayName: { value: props.displayName, enumerable: true }
      });
    }

    defaultNameInCode(): string {
      return Formatter.formatCamelCaseIdentifier(this.displayName);
    }
  }

  return ApiConfigRef;
});
