define(function(require) {
  const Formatter = require('../lib/formatter');

  class ParamType {
    constructor(props) {
      const initialProps = Object.assign({
        name: "",
        needsConfig: false
      }, props);

      const isBuiltIn = ParamType.builtinTypes.includes(initialProps.id) && initialProps.id === initialProps.name;

      Object.defineProperties(this, {
        id: { value: initialProps.id, enumerable: true },
        exportId: { value: initialProps.exportId, enumerable: true },
        isBuiltIn: { value: isBuiltIn, enumerable: true },
        name: { value: initialProps.name, enumerable: true },
        needsConfig: { value: initialProps.needsConfig, enumerable: true }
      });
    }

    isNumberType() {
      return this.isBuiltIn && this.id === "Number";
    }

    isYesNoType() {
      return this.isBuiltIn && this.id === "Yes/No";
    }

    formatValue(value) {
      if (this.isNumberType()) {
        return Formatter.formatPossibleNumber(value);
      } else {
        return value;
      }
    }

    getDefaultValue() {
      if (this.isYesNoType()) {
        return "true";
      } else {
        return "";
      }
    }

    getInputPlaceholder() {
      if (this.isNumberType()) {
        return "Enter a number";
      } else {
        return null;
      }
    }

    getOptions() {
      if (this.isBuiltIn && this.id === "Yes/No") {
        return [{
          label: "Yes",
          value: "true"
        }, {
          label: "No",
          value: "false"
        }];
      } else {
        return null;
      }
    }

    clone(newProps) {
      return new ParamType(Object.assign({}, this, newProps));
    }

    static fromJson(props) {
      return new ParamType(props);
    }
  }

  ParamType.builtinTypes = ['Text', 'Number', 'Yes/No'];

  return ParamType;
});
