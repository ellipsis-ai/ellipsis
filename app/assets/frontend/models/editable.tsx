import DeepEqual from '../lib/deep_equal';

class Editable {
    id?: string | null;
    groupId: string;
    teamId: string;
    isNew?: boolean | null;
    name: string | null;
    description?: string | null;
    functionBody: string;
    exportId?: string | null;
    editorScrollPosition?: number | null;
    createdAt?: number | null;

    constructor(
      id: string | null,
      groupId: string,
      teamId: string,
      isNew: boolean | null,
      name: string | null,
      description: string | null,
      functionBody: string,
      exportId: string | null,
      editorScrollPosition: number | null,
      createdAt: number | null
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

    sortKeyForExisting(): string | null {
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

    getPersistentId(): string {
      throw "Needs to be implemented in subclasses";
    }

    clone(props: {}): Editable {
      throw "Needs to be implemented in subclasses";
    }

    // Used by JSON.stringify for submitting data to the server
    toJSON() {
      return this.clone({
        editorScrollPosition: null
      });
    }

    forEqualityComparison() {
      return this.toJSON();
    }

    isIdenticalToVersion<T extends Editable>(version: T): boolean {
      return DeepEqual.isEqual(this.forEqualityComparison(), version.forEqualityComparison());
    }
}

export default Editable;

