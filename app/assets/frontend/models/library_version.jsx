// @flow
import type {Diffable, DiffableProp} from "./diffs";
import type BehaviorGroup from "./behavior_group";

import Editable from './editable';

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
      editorScrollPosition: ?number,
      createdAt: ?number
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
        editorScrollPosition,
        createdAt
      );

      Object.defineProperties(this, {
        functionBody: { value: functionBody, enumerable: true },
        libraryId: { value: libraryId, enumerable: true }
      });
    }

    sortKeyForExisting(): ?string {
      return this.name;
    }

    diffLabel(): string {
      const itemLabel = this.itemLabel();
      const kindLabel = this.kindLabel();
      return itemLabel ? `${kindLabel} “${itemLabel}”` : `unnamed ${kindLabel}`;
    }

    itemLabel(): ?string {
      return this.name;
    }

    kindLabel(): string {
      return "library";
    }

    getIdForDiff(): string {
      return this.exportId || "";
    }

    diffProps(): Array<DiffableProp> {
      return [{
        name: "Name",
        value: this.name || ""
      }, {
        name: "Description",
        value: this.description || ""
      }, {
        name: "Code",
        value: this.functionBody,
        isCode: true
      }];
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

    buildUpdatedGroupFor(group: BehaviorGroup, props: {}): BehaviorGroup {
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

    clone(props: {}): LibraryVersion {
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
        props.editorScrollPosition,
        props.createdAt
      );
    }

    static defaultLibraryCode(): string {
      return (
`// Write a Node.js (6.10.2) function that returns code you want to share.
// Other components in your skill can require the library to use the code.
//
// Example:
//
// return {
//   getANumber: function() {
//     return Math.floor(Math.random() * 10);
//   }
// };
`
      );
    }

}

export default LibraryVersion;

