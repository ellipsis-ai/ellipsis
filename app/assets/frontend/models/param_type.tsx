import Formatter from '../lib/formatter';

const builtinTypes: Array<string> = ['Text', 'Number', 'Yes/No'];

export interface ParamTypeJson {
  id?: Option<string>
  exportId: string
  name: string
  needsConfig?: Option<boolean>
  typescriptType?: Option<string>
}

interface ParamTypeInterface extends ParamTypeJson {}

class ParamType implements ParamTypeInterface {
  readonly isBuiltIn: boolean;
  readonly name: string;
  readonly needsConfig: boolean;
  readonly typescriptType: string;

  constructor(
    readonly id: Option<string>,
    readonly exportId: string,
    maybeTypescriptType: Option<string>,
    maybeName?: Option<string>,
    maybeNeedsConfig?: Option<boolean>
  ) {
      const isBuiltIn = id && builtinTypes.includes(id) && id === maybeName;

      Object.defineProperties(this, {
        id: { value: id, enumerable: true },
        exportId: { value: exportId, enumerable: true },
        isBuiltIn: { value: isBuiltIn, enumerable: true },
        name: { value: (typeof maybeName === "string" ? maybeName : ""), enumerable: true },
        needsConfig: { value: (typeof maybeNeedsConfig === "boolean" ? maybeNeedsConfig : false), enumerable: true },
        typescriptType: { value: (typeof maybeTypescriptType === "string" ? maybeTypescriptType : "any"), enumerable: true }
      });
    }

    isNumberType(): boolean {
      return this.isBuiltIn && this.id === "Number";
    }

    isYesNoType(): boolean {
      return this.isBuiltIn && this.id === "Yes/No";
    }

    formatValue(value: string): string {
      if (this.isNumberType()) {
        return Formatter.formatPossibleNumber(value);
      } else {
        return value;
      }
    }

    getDefaultValue(): string {
      if (this.isYesNoType()) {
        return "true";
      } else {
        return "";
      }
    }

    getInputPlaceholder(): Option<string> {
      if (this.isNumberType()) {
        return "Enter a number";
      } else {
        return null;
      }
    }

    getOptions(): Option<Array<{ label: string, value: string }>> {
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

    clone(newProps: Partial<ParamTypeInterface>): ParamType {
      return ParamType.fromProps(Object.assign({}, this, newProps));
    }

    static fromProps(props: ParamTypeInterface): ParamType {
      return new ParamType(
        props.id,
        props.exportId,
        props.typescriptType,
        props.name,
        props.needsConfig
      );
    }

    static fromJson(props: ParamTypeJson): ParamType {
      return ParamType.fromProps(props);
    }

    static typescriptTypeForDataTypes(): string {
      return `{
  id: string,
  label: string,
  [k: string]: any
}`;
    }
  }

export default ParamType;

