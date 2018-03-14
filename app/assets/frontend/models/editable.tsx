import DeepEqual from '../lib/deep_equal';

export interface EditableJson {
  id?: Option<string>;
  groupId: string;
  teamId: string;
  isNew?: Option<boolean>;
  name?: Option<string>;
  description?: Option<string>;
  functionBody: string;
  exportId?: Option<string>;
  editorScrollPosition?: Option<number>;
  createdAt?: Option<number>;
}

export interface EditableInterface extends EditableJson {}

abstract class Editable implements EditableInterface {
  constructor(
    readonly id: Option<string>,
    readonly groupId: string,
    readonly teamId: string,
    readonly isNew: Option<boolean>,
    readonly name: Option<string>,
    readonly description: Option<string>,
    readonly functionBody: string,
    readonly exportId: Option<string>,
    readonly editorScrollPosition: Option<number>,
    readonly createdAt: Option<number>
  ) {
      Object.defineProperties(this, {
        id: { value: id, enumerable: true },
        groupId: { value: groupId, enumerable: true },
        teamId: { value: teamId, enumerable: true },
        isNew: { value: isNew, enumerable: true },
        name: { value: name, enumerable: true },
        description: { value: description, enumerable: true },
        functionBody: { value: functionBody, enumerable: true },
        exportId: { value: exportId, enumerable: true },
        editorScrollPosition: { value: editorScrollPosition, enumerable: true },
        createdAt: {value: createdAt, enumerable: true }
      });
  }

    isBehaviorVersion(): boolean {
      return false;
    }

    isDataType(): boolean {
      return false;
    }

    isLibraryVersion(): boolean {
      return false;
    }

    timestampForAlphabeticalSort(): string {
      const timestampString = this.createdAt ? Number(new Date(this.createdAt)).toString() : "";
      const pad = new Array(16).join("0");
      return pad.substring(0, pad.length - timestampString.length) + timestampString;
    }

    sortKeyForExisting(): Option<string> {
      return null; // override in subclasses
    }

    sortKey(): string {
      if (this.isNew) {
        return "Z" + this.timestampForAlphabeticalSort();
      } else {
        return "A" + (this.sortKeyForExisting() || this.timestampForAlphabeticalSort());
      }
    }

    namePlaceholderText(): string {
      return "Item name";
    }

    cloneActionText(): string {
      return "Clone item…";
    }

    deleteActionText(): string {
      return "Delete item…";
    }

    confirmDeleteText(): string {
      return "Are you sure you want to delete this item?";
    }

    getEditorTitle(): string {
      return this.isNew ? this.getNewEditorTitle() : this.getExistingEditorTitle();
    }

    getNewEditorTitle(): string {
      return "New item";
    }

    getExistingEditorTitle(): string {
      return "Edit item";
    }

    getName(): string {
      return this.name || "";
    }

    getDescription(): string {
      return this.description || "";
    }

    cancelNewText(): string {
      return "Cancel new item";
    }

    includesText(queryString: string): boolean {
      var lowercase = queryString.toLowerCase().trim();
      return this.getName().toLowerCase().includes(lowercase) ||
        this.getDescription().toLowerCase().includes(lowercase);
    }

    abstract getPersistentId(): string

    abstract clone(props: Partial<EditableInterface>): Editable

    // Used by JSON.stringify for submitting data to the server
    toJSON(): Editable {
      return this.clone({
        editorScrollPosition: null
      });
    }

    abstract forEqualityComparison(): Editable

    isIdenticalToVersion<T extends Editable>(version: T): boolean {
      return DeepEqual.isEqual(this.forEqualityComparison(), version.forEqualityComparison());
    }
}

export default Editable;

