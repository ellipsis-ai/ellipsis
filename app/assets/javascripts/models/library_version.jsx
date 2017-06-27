define(function(require) {
  const Editable = require('./editable');

  class LibraryVersion extends Editable {
    constructor(props) {
      super(props);

      Object.defineProperties(this, {
        libraryId: { value: props.libraryId, enumerable: true }
      });
    }

    namePlaceholderText() {
      return "Library name";
    }

    cloneActionText() {
      return "Clone library…";
    }

    deleteActionText() {
      return "Delete library…";
    }

    confirmDeleteText() {
      return "Are you sure you want to delete this library?";
    }

    getNewEditorTitle() {
      return "New library";
    }

    getExistingEditorTitle() {
      return "Edit library";
    }

    buildUpdatedGroupFor(group, props) {
      const updated = this.clone(props);
      const updatedVersions = group.libraryVersions.
        filter(ea => ea.libraryId !== updated.libraryId ).
        concat([updated]);
      return group.clone({ libraryVersions: updatedVersions });
    }

    getPersistentId() {
      return this.libraryId;
    }

    isLibraryVersion() {
      return true;
    }

    clone(props) {
      return new LibraryVersion(Object.assign({}, this, props));
    }

  }

  return LibraryVersion;
});
