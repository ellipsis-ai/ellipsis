import DeepEqual from '../lib/deep_equal';
import {Timestamp} from "../lib/formatter";
import BehaviorVersion from "./behavior_version";
import BehaviorGroup from "./behavior_group";
import LibraryVersion from "./library_version";

export interface EditableJson {
  id?: Option<string>;
  groupId?: Option<string>;
  teamId?: Option<string>;
  isNew?: Option<boolean>;
  name?: Option<string>;
  description?: Option<string>;
  functionBody: string;
  exportId?: Option<string>;
  editorScrollPosition?: Option<number>;
  createdAt?: Option<Timestamp>;
}

export interface EditableInterface extends EditableJson {}

abstract class Editable implements EditableInterface {
  constructor(
    readonly id: Option<string>,
    readonly groupId: Option<string>,
    readonly teamId: Option<string>,
    readonly isNew: Option<boolean>,
    readonly name: Option<string>,
    readonly description: Option<string>,
    readonly functionBody: string,
    readonly exportId: Option<string>,
    readonly editorScrollPosition: Option<number>,
    readonly createdAt: Option<Timestamp>
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

    isBehaviorVersion(): this is BehaviorVersion {
      return false;
    }

    isDataType(): boolean {
      return false;
    }

    isTest(): boolean {
      return false;
    }

    isLibraryVersion(): this is LibraryVersion {
      return false;
    }

    timestampToNumberString(): string {
      const t = this.createdAt;
      if (typeof t === "string") {
        return Number(new Date(t)).toString();
      } else if (typeof t === "number") {
        return Number(new Date(t)).toString();
      } else if (t instanceof Date) {
        return Number(t).toString();
      } else {
        return "";
      }
    }

    timestampForAlphabeticalSort(): string {
      const timestampString = this.timestampToNumberString();
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

    abstract namePlaceholderText(): string

    abstract cloneActionText(): string

    abstract deleteActionText(): string

    abstract confirmDeleteText(): string

    getEditorTitle(): string {
      return this.isNew ? this.getNewEditorTitle() : this.getExistingEditorTitle();
    }

    abstract getNewEditorTitle(): string

    abstract getExistingEditorTitle(): string

    getName(): string {
      return this.name || "";
    }

    getDescription(): string {
      return this.description || "";
    }

    abstract cancelNewText(): string

    includesText(queryString: string): boolean {
      var lowercase = queryString.toLowerCase().trim();
      return this.getName().toLowerCase().includes(lowercase) ||
        this.getDescription().toLowerCase().includes(lowercase);
    }

    getFunctionBody(): string {
      return this.functionBody || "";
    }

    getEnvVarNamesInFunction(): Array<string> {
      const vars: Set<string> = new Set();
      const body = this.getFunctionBody();
      const matches = body.match(/ellipsis\.env\.([A-Z_][0-9A-Z_]*)/g);
      if (matches) {
        matches.forEach((match) => {
          vars.add(match.replace(/^ellipsis\.env\./, ""));
        });
      }
      return Array.from(vars);
    }

    abstract getPersistentId(): string

    abstract buildUpdatedGroupFor<T extends EditableInterface>(group: BehaviorGroup, props: Partial<T>): BehaviorGroup

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

    abstract icon(): string
}

export default Editable;

