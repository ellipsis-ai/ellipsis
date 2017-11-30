// @flow

export interface Diffable {
  diffLabel(): string;
}

export interface Diff {
  displayText(): string;
}

export type TextPartKind = "added" | "removed" | "unchanged";

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

  const TEXT_ADDED = "added";
  const TEXT_REMOVED = "removed";
  const TEXT_UNCHANGED = "unchanged";

  class TextPart {
    value: string;
    kind: TextPartKind;

    constructor(value: string, added: ?boolean, removed: ?boolean) {
      if (added && removed) {
        throw "Can't be both added and removed";
      } else {
        const kind = added ? TEXT_ADDED : (removed ? TEXT_REMOVED : TEXT_UNCHANGED);
        Object.defineProperties(this, {
          value: { value: value, enumerable: true },
          kind: { value: kind, enumerable: true }
        });
      }
    }

    isAdded(): boolean {
      return this.kind === TEXT_ADDED;
    }

    isRemoved(): boolean {
      return this.kind === TEXT_REMOVED;
    }

  }

  class TextPropertyDiff extends PropertyDiff<string> {
    parts: Array<TextPart>;

    constructor(label: string, original: string, modified: string) {
      super(label, original, modified);
      const parts = JsDiff.diffChars(original, modified, {}).map(ea => {
        return new TextPart(ea.value, ea.added, ea.removed);
      });
      Object.defineProperties(this, {
        parts: { value: parts, enumerable: true }
      });
    }

    displayText(): string {
      const partsString = this.parts.map(ea => {
        const text = ea.value;
        if (ea.isAdded()) {
          return `[+${text}]`;
        } else if (ea.isRemoved()) {
          return `[-${text}]`;
        } else {
          return text;
        }
      }).join("");
      return `${this.label}: ${partsString}`;
    }

    static maybeFor(label: string, maybeOriginal: ?string, maybeModified: ?string): ?TextPropertyDiff {
      const original = maybeOriginal || "";
      const modified = maybeModified || "";
      if (original === modified) {
        return null;
      } else {
        return new TextPropertyDiff(label, original, modified);
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

  function diffsFor<T: Diffable>(first: T, second: T, collectionProperty: string, idProperty: string): Array<Diff> {
    const firstItems = (first: Object)[collectionProperty];
    const secondItems = (second: Object)[collectionProperty];
    const myIds = firstItems.map(ea => ea[idProperty]);
    const otherIds = secondItems.map(ea => ea[idProperty]);

    const modifiedIds = myIds.filter(ea => otherIds.indexOf(ea) >= 0);
    const addedIds = myIds.filter(ea => otherIds.indexOf(ea) === -1);
    const removedIds = otherIds.filter(ea => myIds.indexOf(ea) === -1);

    const added: Array<Diff> = addedIds.map(eaId => {
      return firstItems.find(ea => ea[idProperty] === eaId);
    })
      .filter(ea => !!ea)
      .map(ea => new AddedDiff(ea));

    const removed: Array<Diff> = removedIds.map(eaId => {
      return secondItems.find(ea => ea[idProperty] === eaId);
    })
      .filter(ea => !!ea)
      .map(ea => new RemovedDiff(ea));

    const modified: Array<Diff> = [];
    modifiedIds.forEach(eaId => {
      const firstItem = firstItems.find(ea => ea[idProperty] === eaId);
      const secondItem = secondItems.find(ea => ea[idProperty] === eaId);
      if (firstItem) {
        const diff = firstItem.maybeDiffFor(secondItem);
        if (diff) {
          modified.push(diff);
        }
      }
    });

    return added.concat(removed.concat(modified));
  }

  return {
    'diffsFor': diffsFor,
    'AddedDiff': AddedDiff,
    'BooleanPropertyDiff': BooleanPropertyDiff,
    'RemovedDiff': RemovedDiff,
    'ModifiedDiff': ModifiedDiff,
    'TextPropertyDiff': TextPropertyDiff
  };
});
