import {Diffable, DiffableProp} from "./diffs";

export interface TriggerJson {
  text: string;
  isRegex: boolean;
  requiresMention: boolean;
  caseSensitive: boolean;
}

interface TriggerInterface extends TriggerJson {}

class Trigger implements Diffable, TriggerInterface {
  readonly isRegex: boolean;
  readonly requiresMention: boolean;
  readonly caseSensitive: boolean;

  constructor(
    readonly text: string,
    maybeIsRegex: boolean | null,
    maybeRequiresMention: boolean | null
  ) {
      const isRegex: boolean = typeof maybeIsRegex === "boolean" ? Boolean(maybeIsRegex) : false;
      const requiresMention: boolean = typeof maybeRequiresMention === "boolean" ? Boolean(maybeRequiresMention) : true;
      Object.defineProperties(this, {
        /* Case sensitivity disabled in the UI, so force it to be false */
        caseSensitive: {
          value: false,
          enumerable: true
        },
        isRegex: {
          value: isRegex,
          enumerable: true
        },
        requiresMention: {
          value: requiresMention,
          enumerable: true
        },
        text: {
          value: text,
          enumerable: true
        }
      });
    }

    diffLabel(): string {
      const itemLabel = this.itemLabel();
      const kindLabel = this.kindLabel();
      return itemLabel ? `${kindLabel} “${itemLabel}”` : `empty ${kindLabel}`;
    }

    itemLabel(): string | null {
      return this.text;
    }

    kindLabel(): string {
      return "trigger";
    }

    getIdForDiff(): string {
      return this.text;
    }

    diffProps(): Array<DiffableProp> {
      return [{
        name: "Treat as regex pattern",
        value: this.isRegex
      }, {
        name: "Require user to mention Ellipsis",
        value: this.requiresMention
      }];
    }

    getText(): string {
      return this.text;
    }

    paramNames(): Array<string> {
      let names: Array<string> = [];
      const matches = this.getText().match(/\{.+?\}/g);
      if (!this.isRegex && matches) {
        names = matches.map((name) => name.replace(/^\{|\}$/g, ''));
      }
      return names;
    }

    displayText(): string {
      return this.getText();
    }

    hasNonRegexParams(): boolean {
      return !this.isRegex && /\{.+?\}/.test(this.getText());
    }

    usesInputName(name: string): boolean {
      return !this.isRegex && this.getText().includes(`{${name}}`);
    }

    capturesInputIndex(index: number): boolean {
      if (!this.isRegex) {
        return false;
      }
      var matches = this.getText().match(/^\(.+?\)|[^\\]\(.*?[^\\]\)/g);
      return !!(matches && matches[index]);
    }

    hasRegexCapturingParens(): boolean {
      return this.isRegex && /\(.+?\)/.test(this.getText());
    }

    getTextWithNewInputName(oldName: string, newName: string): string {
      if (!this.isRegex) {
        return this.getText().split(`{${oldName}}`).join(`{${newName}}`);
      } else {
        return this.getText();
      }
    }

    clone(props: Partial<TriggerInterface>): Trigger {
      return Trigger.fromProps(Object.assign({}, this, props));
    }

    static fromProps(props: TriggerInterface): Trigger {
      return new Trigger(
        props.text,
        props.isRegex,
        props.requiresMention
      );
    }

    static triggersFromJson(jsonArrayOrNull: Array<TriggerJson> | null) {
      if (jsonArrayOrNull) {
        return jsonArrayOrNull.map((triggerObj) => Trigger.fromProps(triggerObj));
      } else {
        return null;
      }
    }
}

export default Trigger;

