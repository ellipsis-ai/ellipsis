// @flow
define(function(require) {
  const Editable = require('./editable');

  class LibraryVersion extends Editable {
    functionBody: string;
    libraryId: string;

    constructor(
      id: ?string,
      name: ?string,
      description: ?string,
      functionBody: string,
      groupId: string,
      teamId: string,
      libraryId: string,
      exportId: ?string,
      isNew: boolean,
      editorScrollPosition: ?number
    ) {
      super(
        id,
        groupId,
        teamId,
        isNew,
        name,
        description,
        functionBody,
        exportId,
        editorScrollPosition
      );

      Object.defineProperties(this, {
        functionBody: { value: functionBody, enumerable: true },
        libraryId: { value: libraryId, enumerable: true }
      });
    }

    namePlaceholderText(): string {
      return "Library name";
    }

    cloneActionText(): string {
      return "Clone library…";
    }

    deleteActionText(): string {
      return "Delete library…";
    }

    confirmDeleteText(): string {
      return "Are you sure you want to delete this library?";
    }

    getNewEditorTitle(): string {
      return "New library";
    }

    getExistingEditorTitle(): string {
      return "Edit library";
    }

    cancelNewText(): string {
      return "Cancel new library";
    }

    buildUpdatedGroupFor(group, props): LibraryVersion {
      const updated = this.clone(props);
      const updatedVersions = group.libraryVersions.
        filter(ea => ea.libraryId !== updated.libraryId ).
        concat([updated]);
      return group.clone({ libraryVersions: updatedVersions });
    }

    getPersistentId(): string {
      return this.libraryId;
    }

    isLibraryVersion(): boolean {
      return true;
    }

    clone(props): LibraryVersion {
      return LibraryVersion.fromProps(Object.assign({}, this, props));
    }

    static fromProps(props): LibraryVersion {
      return new LibraryVersion(
        props.id,
        props.name,
        props.description,
        props.functionBody,
        props.groupId,
        props.teamId,
        props.libaryId,
        props.exportId,
        props.isNew,
        props.editorScrollPosition
      );
    }

  }

  return LibraryVersion;
});
