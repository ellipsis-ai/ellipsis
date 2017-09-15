define(function(require) {
  const Formatter = require('../lib/formatter');

  class ApiConfigRef {
    constructor(props) {
      Object.defineProperties(this, {
        id: { value: props.id, enumerable: true },
        displayName: { value: props.displayName, enumerable: true }
      });
    }

    defaultNameInCode() {
      return Formatter.formatCamelCaseIdentifier(this.displayName);
    }

    getSelectorLabel() {
      return this.displayName || "meh";
    }
  }

  return ApiConfigRef;
});
