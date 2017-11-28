// @flow
define(function(require) {
  const JsDiff = require("diff");

  class Diff<T> {
    label: string;
    children: Array<Diff<any>>;

    constructor(label: string, children: Array<Diff<any>>) {
      Object.defineProperties(this, {
        label: { value: label, enumerable: true },
        children: { value: children, enumerable: true }
      });
    }

    displayText(): string {
      const content = this.displayTextContent();
      if (content.trim().length) {
        return `${this.label}: ${this.displayTextContent()}`;
      } else {
        return "";
      }
    }

    displayTextContent(): string {
      return this.children.map(ea => ea.displayText()).join("\n");
    }

  }

  class AddedDiff<T> extends Diff<T> {
    item: T;

    constructor(label: string, children: Array<Diff<any>>, item: T) {
      super(label, children);
      Object.defineProperties(this, {
        item: { value: item, enumerable: true }
      });
    }

  }

  class RemovedDiff<T> extends Diff<T> {
    item: T;

    constructor(label: string, children: Array<Diff<any>>, item: T) {
      super(label, children);
      Object.defineProperties(this, {
        item: { value: item, enumerable: true }
      });
    }

  }

  class ModifiedDiff<T> extends Diff<T> {
    original: T;
    modified: T;

    constructor(label: string, children: Array<Diff<any>>, original: T, modified: T) {
      super(label, children);
      Object.defineProperties(this, {
        original: { value: original, enumerable: true },
        modified: { value: modified, enumerable: true }
      });
    }

  }

  class TextDiff extends ModifiedDiff<string> {
    displayTextContent(): string {
      const parts = JsDiff.diffChars(this.original, this.modified, {});
      return parts.map(ea => {
        const text = ea.value;
        if (ea.added) {
          return `[+${text}]`;
        } else if (ea.removed) {
          return `[-${text}]`;
        } else {
          return text;
        }
      }).join("");
    }

    static maybeFor(label: string, maybeOriginal: ?string, maybeModified: ?string): ?TextDiff {
      const original = maybeOriginal || "";
      const modified = maybeModified || "";
      if (original === modified) {
        return null;
      } else {
        return new TextDiff(label, [], original, modified);
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
