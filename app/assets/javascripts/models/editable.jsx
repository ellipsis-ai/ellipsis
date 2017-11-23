define(function(require) {
  var DeepEqual = require('../lib/deep_equal');

  class Editable {

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
        editorScrollPosition: { value: editorScrollPosition || 0, enumerable: true }
      });
    }

    isBehaviorVersion() {
      return false;
    }

    isDataType() {
      return false;
    }

    isLibraryVersion() {
      return false;
    }

    namePlaceholderText() {
      return "Item name";
    }

    cloneActionText() {
      return "Clone item…";
    }

    deleteActionText() {
      return "Delete item…";
    }

    confirmDeleteText() {
      return "Are you sure you want to delete this item?";
    }

    getEditorTitle() {
      return this.isNew ? this.getNewEditorTitle() : this.getExistingEditorTitle();
    }

    getNewEditorTitle() {
      return "New item";
    }

    getExistingEditorTitle() {
      return "Edit item";
    }

    getName() {
      return this.name || "";
    }

    getDescription() {
      return this.description || "";
    }

    cancelNewText() {
      return "Cancel new item";
    }

    includesText(queryString) {
      var lowercase = queryString.toLowerCase().trim();
      return this.getName().toLowerCase().includes(lowercase) ||
        this.getDescription().toLowerCase().includes(lowercase);
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

    isIdenticalToVersion(version) {
      return DeepEqual.isEqual(this.forEqualityComparison(), version.forEqualityComparison());
    }

    static forEqualityComparison(version) {
      return version.forEqualityComparison();
    }
  }

  return Editable;
});
