// @flow

export interface Diffable {
  diffLabel(): string;
}

export interface Diff {
  displayText(): string;
}

define(function(require) {
  const JsDiff = require("diff");

  class AddedOrRemovedDiff<T: Diffable> implements Diff {
    item: T;

    constructor(item: T) {
      Object.defineProperties(this, {
        item: { value: item, enumerable: true }
      });
    }

    displayText(): string {
      return this.label();
    }

    diffType(): string {
      throw "Should be implemented by subclasses";
    }

    label(): string {
      return `${this.diffType()} ${this.item.diffLabel()}`;
    }

  }

  class AddedDiff<T: Diffable> extends AddedOrRemovedDiff<T> {

    diffType(): string {
      return "Added";
    }

  }

  class RemovedDiff<T: Diffable> extends AddedOrRemovedDiff<T> {

    diffType(): string {
      return "Removed";
    }

  }

  class ModifiedDiff<T: Diffable> implements Diff {
    original: T;
    modified: T;
    children: Array<Diff>;

    constructor(children: Array<Diff>, original: T, modified: T) {
      Object.defineProperties(this, {
        original: { value: original, enumerable: true },
        modified: { value: modified, enumerable: true },
        children: { value: children, enumerable: true }
      });
    }

    label(): string {
      return `Modified ${this.original.diffLabel()}`;
    }

    displayText(): string {
      const childDisplayText = this.children.map(ea => ea.displayText()).join("\n");
      return `${this.label()}:\n${childDisplayText}`;
    }

  }

  class PropertyDiff<T> implements Diff {
    label: string;
    original: T;
    modified: T;

    constructor(label: string, original: T, modified: T) {
      Object.defineProperties(this, {
        label: { value: label, enumerable: true },
        original: { value: original, enumerable: true },
        modified: { value: modified, enumerable: true }
      });
    }

    displayText(): string {
      throw "Should be implemented by subclasses";
    }

  }

  class TextDiff extends PropertyDiff<string> {

    displayText(): string {
      const parts = JsDiff.diffChars(this.original, this.modified, {});
      const partsString = parts.map(ea => {
        const text = ea.value;
        if (ea.added) {
          return `[+${text}]`;
        } else if (ea.removed) {
          return `[-${text}]`;
        } else {
          return text;
        }
      }).join("");
      return `${this.label}: ${partsString}`;
    }

    static maybeFor(label: string, maybeOriginal: ?string, maybeModified: ?string): ?TextDiff {
      const original = maybeOriginal || "";
      const modified = maybeModified || "";
      if (original === modified) {
        return null;
      } else {
        return new TextDiff(label, original, modified);
      }
    }
  }

  class BooleanPropertyDiff extends PropertyDiff<boolean> {

    displayText(): string {
      const valueString = this.original ? "false" : "true";
      return `${this.label}: changed to ${valueString}`;
    }

    static maybeFor(label: string, original: boolean, modified: boolean): ?BooleanPropertyDiff {
      if (original === modified) {
        return null;
      } else {
        return new BooleanPropertyDiff(label, original, modified);
      }
    }
  }

  return {
    'AddedDiff': AddedDiff,
    'BooleanPropertyDiff': BooleanPropertyDiff,
    'RemovedDiff': RemovedDiff,
    'ModifiedDiff': ModifiedDiff,
    'TextDiff': TextDiff
  };
});
