import {Diffable, DiffableProp} from "./diffs";

export enum TriggerType {
  MessageSent = "MessageSent",
  ReactionAdded = "ReactionAdded"
}

export interface TriggerJson {
  text: string,
  isRegex: boolean,
  requiresMention: boolean,
  caseSensitive: boolean,
  triggerType: TriggerType
}

export interface TriggerInterface extends TriggerJson {}

class Trigger implements Diffable, TriggerInterface {
  readonly isRegex: boolean;
  readonly requiresMention: boolean;
  readonly caseSensitive: boolean;
  readonly text: string;
  readonly triggerType: TriggerType;

  constructor(
    triggerType: TriggerType,
    maybeText?: Option<string>,
    maybeIsRegex?: Option<boolean>,
    maybeRequiresMention?: Option<boolean>
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
          value: maybeText || "",
          enumerable: true
        },
        triggerType: {
          value: triggerType,
          enumerable: true
        }
      });
    }

    diffLabel(): string {
      const itemLabel = this.itemLabel();
      const kindLabel = this.kindLabel();
      return itemLabel ? `${kindLabel} “${itemLabel}”` : `empty ${kindLabel}`;
    }

    itemLabel(): Option<string> {
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
      }, {
        name: "Trigger type",
        value: this.triggerType,
        isCategorical: true
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
      const text = this.getText();
      if (this.isReactionAddedTrigger()) {
        return `:${text}:`;
      } else if (this.isRegex) {
        return text.replace(/(^\^|\$$)/g, "");
      } else {
        return text;
      }
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

    isMessageSentTrigger(): boolean {
      return this.triggerType === TriggerType.MessageSent;
    }

    isReactionAddedTrigger(): boolean {
      return this.triggerType === TriggerType.ReactionAdded;
    }

    isExactMatchFor(text: string): boolean {
      return this.isMessageSentTrigger() && !this.isRegex && this.text === text ||
        !this.caseSensitive && this.text.toLowerCase() === text.toLowerCase();
    }

    clone(props: Partial<TriggerInterface>): Trigger {
      return Trigger.fromProps(Object.assign({}, this, props));
    }

    static fromJson(props: TriggerJson): Trigger {
      return new Trigger(
        props.triggerType,
        props.text,
        props.isRegex,
        props.requiresMention,
      );
    }

    static fromProps(props: TriggerInterface): Trigger {
      return Trigger.fromJson(props);
    }

    static triggersFromJson(jsonArray: Array<TriggerJson>) {
      return jsonArray.map((triggerObj) => Trigger.fromJson(triggerObj));
    }
}

export default Trigger;

