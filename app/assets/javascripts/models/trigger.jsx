// @flow

import {Diff, Diffable} from "./diffs";

define(function(require) {
  const DeepEqual = require('../lib/deep_equal');
  const diffs = require('./diffs');

  class Trigger implements Diffable {
    text: string;
    isRegex: boolean;
    requiresMention: boolean;
    caseSensitive: boolean;

    constructor(
      text: ?string,
      maybeIsRegex: ?boolean,
      maybeRequiresMention: ?boolean
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
          value: text || "",
          enumerable: true
        }
      });
    }

    diffLabel(): string {
      return `trigger "${this.text}"`;
    }

    isIdenticalTo(other): boolean {
      return DeepEqual.isEqual(this, other);
    }

    maybeDiffFor(other: Trigger): ?diffs.ModifiedDiff<Trigger> {
      if (this.isIdenticalTo(other)) {
        return null;
      } else {
        const children: Array<Diff> = [
          diffs.BooleanPropertyDiff.maybeFor("Treat as regex", this.isRegex, other.isRegex),
          diffs.BooleanPropertyDiff.maybeFor("Require bot mention", this.requiresMention, other.requiresMention)
        ].filter(ea => Boolean(ea));
        return new diffs.ModifiedDiff(children, this, other);
      }
    }

    getText(): string {
      return this.text;
    }

    paramNames(): Array<string> {
      let names = [];
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

    usesInputName(name): boolean {
      return !this.isRegex && this.getText().includes(`{${name}}`);
    }

    capturesInputIndex(index): boolean {
      if (!this.isRegex) {
        return false;
      }
      var matches = this.getText().match(/^\(.+?\)|[^\\]\(.*?[^\\]\)/g);
      return !!(matches && matches[index]);
    }

    hasRegexCapturingParens(): boolean {
      return this.isRegex && /\(.+?\)/.test(this.getText());
    }

    getTextWithNewInputName(oldName, newName): string {
      if (!this.isRegex) {
        return this.getText().split(`{${oldName}}`).join(`{${newName}}`);
      } else {
        return this.getText();
      }
    }

    clone(props): Trigger {
      return Trigger.fromProps(Object.assign({}, this, props));
    }

    static fromProps(props): Trigger {
      return new Trigger(
        props.text,
        props.isRegex,
        props.requiresMention
      );
    }

    static triggersFromJson(jsonArrayOrNull) {
      if (jsonArrayOrNull) {
        return jsonArrayOrNull.map((triggerObj) => Trigger.fromProps(triggerObj));
      } else {
        return null;
      }
    }
  }

  return Trigger;
});
