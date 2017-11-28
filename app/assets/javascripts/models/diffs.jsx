// @flow

export interface Diffable {
  itemType(): string;
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
      return "";
    }

    label(): string {
      return this.item.itemType();
    }

  }

  class AddedDiff<T: Diffable> extends AddedOrRemovedDiff<T> {

    displayText(): string {
      return `Added ${this.label()}`;
    }

  }

  class RemovedDiff<T: Diffable> extends AddedOrRemovedDiff<T> {

    displayText(): string {
      return `Removed ${this.item.itemType()}`;
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

    displayText(): string {
      return this.children.map(ea => ea.displayText()).join("\n");
    }

  }

  class Text implements Diffable {
    value: string;

    constructor(value: string) {
      Object.defineProperties(this, {
        value: { value: value, enumerable: true }
      });
    }

    itemType(): string {
      return "";
    }
  }

  class TextDiff extends ModifiedDiff<Text> {
    label: string;

    constructor(label: string, original: Text, modified: Text) {
      super([], original, modified);
      Object.defineProperties(this, {
        label: { value: label, enumerable: true }
      });
    }

    displayText(): string {
      const parts = JsDiff.diffChars(this.original.value, this.modified.value, {});
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
      const originalValue = maybeOriginal || "";
      const modifiedValue = maybeModified || "";
      if (originalValue === modifiedValue) {
        return null;
      } else {
        return new TextDiff(label, new Text(originalValue), new Text(modifiedValue));
      }
    }
  }

  return {
    'AddedDiff': AddedDiff,
    'RemovedDiff': RemovedDiff,
    'ModifiedDiff': ModifiedDiff,
    'TextDiff': TextDiff
  };
});
