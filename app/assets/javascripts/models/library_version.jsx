// @flow
import type {Diff, Diffable} from "./diffs";

define(function(require) {
  const Editable = require('./editable');
  const diffs = require('./diffs');

  class LibraryVersion extends Editable implements Diffable {
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

    diffLabel(): string {
      return `library "${this.name}"`;
    }

    getIdForDiff(): string {
      return this.libraryId;
    }

    maybeDiffFor(other: LibraryVersion): ?diffs.ModifiedDiff<LibraryVersion> {
      if (this.isIdenticalToVersion(other)) {
        return null;
      } else {
        const children: Array<Diff> = [
          diffs.TextPropertyDiff.maybeFor("Name", this.name, other.name),
          diffs.TextPropertyDiff.maybeFor("Description", this.description, other.description),
          diffs.TextPropertyDiff.maybeFor("Code", this.functionBody, other.functionBody)
        ].filter(ea => Boolean(ea));
        return new diffs.ModifiedDiff(children, this, other);
      }
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
        props.libraryId,
        props.exportId,
        props.isNew,
        props.editorScrollPosition
      );
    }

  }

  return LibraryVersion;
});
