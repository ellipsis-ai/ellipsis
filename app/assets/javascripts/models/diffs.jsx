// @flow

export interface Diff {
  displayText(): string;
  summaryText(): string;
}

type DiffableParent<T> = {
  mine: T,
  other: T
};

export interface Diffable {
  diffLabel(): string;
  getIdForDiff(): string;

  // TODO: `other` should be a type that implements Diffable; using Diffable directly doesn't allow classes to override with themselves
  // T should probably be a Diffable or HasInputs
  maybeDiffFor<T: *>(other: *, parents?: DiffableParent<T>): ?Diff;
}

// TODO: This is a hack since we can't import BehaviorGroup here or inside BehaviorVersion (because it would be a circular dependency)
export interface HasInputs<T> extends Diffable {
  getInputs(): Array<T>
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

    summaryText(): string {
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
      const original = this.original.diffLabel();
      const modified = this.modified.diffLabel();
      const label = original === modified ? original : `${original} → ${modified}`;
      return `Modified ${label}`;
    }

    summaryText(): string {
      return this.label();
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

    summaryText(): string {
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

    valueIsEmpty(): boolean {
      return !this.value;
    }

  }

  class TextPropertyDiff extends PropertyDiff<string> {
    parts: Array<TextPart>;

    constructor(label: string, original: string, modified: string) {
      super(label, original, modified);
      const parts = JsDiff.diffWordsWithSpace(original, modified, {}).map(ea => {
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

    getTextChangeType(): string {
      const hasAddedParts = this.parts.some((ea) => ea.isAdded());
      const hasRemovedParts = this.parts.some((ea) => ea.isRemoved());
      if (hasAddedParts && hasRemovedParts) {
        return "changed";
      } else if (hasAddedParts) {
        return "added";
      } else if (hasRemovedParts) {
        return "removed";
      } else {
        return "unchanged";
      }
    }

    summaryText(): string {
      return `${this.label} ${this.getTextChangeType()}`;
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
      const valueString = this.original ? "off" : "on";
      return `${this.label}: changed to ${valueString}`;
    }

    summaryText(): string {
      return this.displayText();
    }

    static maybeFor(label: string, original: boolean, modified: boolean): ?BooleanPropertyDiff {
      if (original === modified) {
        return null;
      } else {
        return new BooleanPropertyDiff(label, original, modified);
      }
    }
  }

  class CategoricalPropertyDiff extends PropertyDiff<string> {

    displayText(): string {
      return `${this.label}: changed from ${this.original} to ${this.modified}`;
    }

    summaryText(): string {
      return this.displayText();
    }

    static maybeFor(label: string, original: string, modified: string): ?CategoricalPropertyDiff {
      if (original === modified) {
        return null;
      } else {
        return new CategoricalPropertyDiff(label, original, modified);
      }
    }

  }

  function diffsFor<T: Diffable, I, P: HasInputs<I>>(originalItems: Array<T>, newItems: Array<T>, parents?: DiffableParent<P>): Array<Diff> {
    const originalIds = originalItems.map(ea => ea.getIdForDiff());
    const newIds = newItems.map(ea => ea.getIdForDiff());

    const modifiedIds = originalIds.filter(ea => newIds.indexOf(ea) >= 0);
    const addedIds = newIds.filter(ea => originalIds.indexOf(ea) === -1);
    const removedIds = originalIds.filter(ea => newIds.indexOf(ea) === -1);

    const added: Array<Diff> = [];
    addedIds.forEach(eaId => {
      const item = newItems.find(ea => ea.getIdForDiff() === eaId);
      if (item) {
        added.push(new AddedDiff(item));
      }
    });

    const removed: Array<Diff> = [];
    removedIds.forEach(eaId => {
      const item = originalItems.find(ea => ea.getIdForDiff() === eaId);
      if (item) {
        removed.push(new RemovedDiff(item));
      }
    });

    const modified: Array<Diff> = [];
    modifiedIds.forEach(eaId => {
      const originalItem = originalItems.find(ea => ea.getIdForDiff() === eaId);
      const newItem = newItems.find(ea => ea.getIdForDiff() === eaId);
      if (originalItem && newItem) {
        const diff = originalItem.maybeDiffFor(newItem, parents);
        if (diff) {
          modified.push(diff);
        }
      }
    });

    return added.concat(removed.concat(modified));
  }

  return {
    diffsFor: diffsFor,
    AddedOrRemovedDiff: AddedOrRemovedDiff,
    AddedDiff: AddedDiff,
    BooleanPropertyDiff: BooleanPropertyDiff,
    CategoricalPropertyDiff: CategoricalPropertyDiff,
    RemovedDiff: RemovedDiff,
    ModifiedDiff: ModifiedDiff,
    TextPart: TextPart,
    TextPropertyDiff: TextPropertyDiff,
    constants: {
      TEXT_ADDED: TEXT_ADDED,
      TEXT_REMOVED: TEXT_REMOVED,
      TEXT_UNCHANGED: TEXT_UNCHANGED
    }
  };
});
