// @flow
define(function(require) {
  var DeepEqual = require('../lib/deep_equal');

  class Editable {
    id: ?string;
    groupId: string;
    teamId: string;
    isNew: boolean;
    name: ?string;
    description: ?string;
    functionBody: string;
    exportId: ?string;
    editorScrollPosition: ?number;

    constructor(
      id: ?string,
      groupId: string,
      teamId: string,
      isNew: boolean,
      name: ?string,
      description: ?string,
      functionBody: string,
      exportId: ?string,
      editorScrollPosition: ?number
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
        editorScrollPosition: { value: editorScrollPosition, enumerable: true }
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

    includesText(queryString): boolean {
      var lowercase = queryString.toLowerCase().trim();
      return this.getName().toLowerCase().includes(lowercase) ||
        this.getDescription().toLowerCase().includes(lowercase);
    }

    clone(props): Editable {
      throw "Needs to be implemented in subclasses";
    }

    // Used by JSON.stringify for submitting data to the server
    toJSON() {
      return this.clone({
        editorScrollPosition: null
      });
    }

    forEqualityComparison() {
      return this.toJSON().clone({
        id: null,
        isNew: null
      });
    }

    isIdenticalToVersion(version): boolean {
      return DeepEqual.isEqual(this.forEqualityComparison(), version.forEqualityComparison());
    }

    static forEqualityComparison(version) {
      return version.forEqualityComparison();
    }
  }

  return Editable;
});
