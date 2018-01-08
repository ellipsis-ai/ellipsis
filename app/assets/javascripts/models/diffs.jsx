// @flow

/* eslint-disable no-use-before-define */
export interface Diff {
  displayText(): string;
  summaryText(): string;
}

// TODO: This is a hack since we can't import BehaviorGroup here or inside BehaviorVersion (because it would be a circular dependency)
export interface HasInputs {
  getInputs<T>(): Array<T>
}

export type DiffableProp = {
  name: string,
  value: Array<Diffable> | string | boolean,
  parent?: HasInputs,
  isCategorical?: boolean,
  isCode?: boolean
}

export interface Diffable {
  diffLabel(): string;
  getIdForDiff(): string;
  diffProps(parent?: HasInputs): Array<DiffableProp>;
}

export type DiffableParent = {
  mine: HasInputs,
  other: HasInputs
};

export type TextPartKind = "added" | "removed" | "unchanged";
/* eslint-enable no-use-before-define */

define(function(require) {
  const JsDiff = require("diff");
  const DeepEqual = require('../lib/deep_equal');

  class OrderingDiff<T: Diffable> implements Diff {
    beforeItems: Array<T>;
    afterItems: Array<T>;

    constructor(beforeItems: Array<T>, afterItems: Array<T>) {
      Object.defineProperties(this, {
        beforeItems: { value: beforeItems, enumerable: true },
        afterItems: { value: afterItems, enumerable: true }
      });
    }

    displayText(): string {
      return this.label();
    }

    summaryText(): string {
      return this.label();
    }

    textForItems(items: Array<T>): string {
      return items.map(ea => ea.diffLabel()).join(", ");
    }

    label(): string {
      return `Changed order: ${this.textForItems(this.beforeItems)} to ${this.textForItems(this.afterItems)}`;
    }
  }

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

  type TextPropertyOptions = {
    isCode?: boolean
  };

  class TextPropertyDiff extends PropertyDiff<string> {
    parts: Array<TextPart>;
    isCode: boolean;

    constructor(label: string, original: string, modified: string, options?: TextPropertyOptions) {
      super(label, original, modified);
      const parts = JsDiff.diffWordsWithSpace(original, modified, {}).map(ea => {
        return new TextPart(ea.value, ea.added, ea.removed);
      });
      Object.defineProperties(this, {
        parts: { value: parts, enumerable: true },
        isCode: { value: Boolean(options && options.isCode), enumerable: true }
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

    static maybeFor(label: string, maybeOriginal: ?string, maybeModified: ?string, options?: TextPropertyOptions): ?TextPropertyDiff {
      const original = maybeOriginal || "";
      const modified = maybeModified || "";
      if (original === modified) {
        return null;
      } else {
        return new TextPropertyDiff(label, original, modified, options);
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

  function diffsFor<T: Diffable>(originalItems: Array<T>, newItems: Array<T>, parents: ?DiffableParent): Array<Diff> {
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
        const diff = maybeDiffFor(originalItem, newItem, parents);
        if (diff) {
          modified.push(diff);
        }
      }
    });

    return added.concat(removed.concat(modified));
  }

  function maybeParentsFor(originalProp: DiffableProp, modifiedProp: ?DiffableProp): ?DiffableParent {
    if (originalProp.parent && modifiedProp && modifiedProp.parent) {
      return { mine: originalProp.parent, other: modifiedProp.parent };
    }
  }

  function flattenDiffs(someDiffs: Array<?Diff | Array<Diff>>): Array<Diff> {
    return someDiffs.reduce((a, b) => {
      return b ? a.concat(b) : a;
    }, []);
  }

  function idsFor(items: Array<Diffable>): Array<string> {
    return items.map(ea => ea.getIdForDiff());
  }

  function orderingDiffsFor(originalItems: Array<Diffable>, newItems: Array<Diffable>): Array<OrderingDiff<*>> {
    const originalIds = idsFor(originalItems);
    const newIds = idsFor(newItems);
    const originalIdsWithoutRemoved = originalIds.filter(ea => newIds.includes(ea));

    const newIdsWithoutAdded = newIds.filter(ea => originalIds.includes(ea));

    if (DeepEqual.isEqual(originalIdsWithoutRemoved, newIdsWithoutAdded)) {
      return [];
    } else {
      return [new OrderingDiff(originalItems, newItems)];
    }
  }

  function maybeDiffFor(original: Diffable, modified: Diffable, parents: ?DiffableParent): ?ModifiedDiff<*> {
    const originalProps = parents ? original.diffProps(parents.mine) : original.diffProps();
    const modifiedProps = parents ? modified.diffProps(parents.other) : modified.diffProps();
    const unflattenedDiffs: Array<?Diff | Array<Diff>> = originalProps.map((originalProp) => {
      const propName = originalProp.name;
      const originalValue = originalProp.value;
      const modifiedProp = modifiedProps.find((otherProp) => otherProp.name === originalProp.name);
      const modifiedValue = modifiedProp ? modifiedProp.value : null;
      if (typeof originalValue === "string") {
        const modifiedString = modifiedValue ? String(modifiedValue) : "";
        return originalProp.isCategorical ?
          CategoricalPropertyDiff.maybeFor(propName, originalValue, modifiedString) :
          TextPropertyDiff.maybeFor(propName, originalValue, modifiedString, { isCode: Boolean(originalProp.isCode) });
      } else if (typeof originalValue === "boolean") {
        return BooleanPropertyDiff.maybeFor(propName, originalValue, Boolean(modifiedValue));
      } else if (Array.isArray(originalValue)) {
        const modifiedArray = Array.isArray(modifiedValue) ? modifiedValue : [];
        const orderingDiffs = orderingDiffsFor(originalValue, modifiedArray);
        return diffsFor(originalValue, modifiedArray, maybeParentsFor(originalProp, modifiedProp)).concat(orderingDiffs);
      } else {
        return null;
      }
    });
    const flattened = flattenDiffs(unflattenedDiffs);
    if (flattened.length === 0) {
      return null;
    } else {
      return new ModifiedDiff(flattened, original, modified);
    }
  }

  return {
    maybeDiffFor: maybeDiffFor,
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
