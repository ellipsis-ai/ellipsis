import {Diffable, DiffableProp} from './diffs';

import BehaviorGroupDeployment, {BehaviorGroupDeploymentJson} from './behavior_group_deployment';
import BehaviorGroupMetaData, {BehaviorGroupMetaDataJson} from "./behavior_group_meta_data";
import BehaviorVersion, {BehaviorVersionJson} from './behavior_version';
import Editable from './editable';
import LibraryVersion, {LibraryVersionJson} from './library_version';
import Input, {InputJson} from './input';
import DeepEqual from '../lib/deep_equal';
import {RequiredAWSConfig, RequiredAWSConfigJson} from './aws';
import {RequiredOAuth1Application, RequiredOAuth1ApplicationJson} from './oauth1';
import {RequiredOAuth2Application, RequiredOAuth2ApplicationJson} from './oauth2';
import {RequiredSimpleTokenApi, RequiredSimpleTokenApiJson} from './simple_token';
import User, {UserJson} from './user';
import ParamType from "./param_type";
import {Timestamp} from "../lib/formatter";
import {default as LinkedGithubRepo, LinkedGitHubRepoJson} from "./linked_github_repo";

const ONE_MINUTE = 60000;

export interface BehaviorGroupJson {
  id?: Option<string>;
  teamId?: Option<string>;
  name?: Option<string>;
  icon?: Option<string>;
  description?: Option<string>;
  actionInputs: Array<InputJson>;
  dataTypeInputs: Array<InputJson>;
  behaviorVersions: Array<BehaviorVersionJson>;
  libraryVersions: Array<LibraryVersionJson>;
  requiredAWSConfigs: Array<RequiredAWSConfigJson>;
  requiredOAuth1ApiConfigs: Array<RequiredOAuth1ApplicationJson>;
  requiredOAuth2ApiConfigs: Array<RequiredOAuth2ApplicationJson>;
  requiredSimpleTokenApis: Array<RequiredSimpleTokenApiJson>;
  createdAt?: Option<Timestamp>;
  exportId?: Option<string>;
  author?: Option<UserJson>;
  gitSHA?: Option<string>;
  deployment?: Option<BehaviorGroupDeploymentJson>;
  metaData?: Option<BehaviorGroupMetaDataJson>;
  isManaged: boolean;
  managedContact?: Option<UserJson>;
  linkedGithubRepo?: Option<LinkedGitHubRepoJson>;
}

interface BehaviorGroupInterface extends BehaviorGroupJson {
  actionInputs: Array<Input>;
  dataTypeInputs: Array<Input>;
  behaviorVersions: Array<BehaviorVersion>;
  libraryVersions: Array<LibraryVersion>;
  requiredAWSConfigs: Array<RequiredAWSConfig>;
  requiredOAuth1ApiConfigs: Array<RequiredOAuth1Application>;
  requiredOAuth2ApiConfigs: Array<RequiredOAuth2Application>;
  requiredSimpleTokenApis: Array<RequiredSimpleTokenApi>;
  author?: Option<User>;
  deployment?: Option<BehaviorGroupDeployment>;
  metaData?: Option<BehaviorGroupMetaData>;
  isManaged: boolean;
  managedContact?: Option<User>;
  linkedGithubRepo?: Option<LinkedGithubRepo>;
}

class BehaviorGroup implements Diffable, BehaviorGroupInterface {
  constructor(
    readonly id: Option<string>,
    readonly teamId: Option<string>,
    readonly name: Option<string>,
    readonly icon: Option<string>,
    readonly description: Option<string>,
    readonly actionInputs: Array<Input>,
    readonly dataTypeInputs: Array<Input>,
    readonly behaviorVersions: Array<BehaviorVersion>,
    readonly libraryVersions: Array<LibraryVersion>,
    readonly requiredAWSConfigs: Array<RequiredAWSConfig>,
    readonly requiredOAuth1ApiConfigs: Array<RequiredOAuth1Application>,
    readonly requiredOAuth2ApiConfigs: Array<RequiredOAuth2Application>,
    readonly requiredSimpleTokenApis: Array<RequiredSimpleTokenApi>,
    readonly createdAt: Option<Timestamp>,
    readonly exportId: Option<string>,
    readonly author: Option<User>,
    readonly gitSHA: Option<string>,
    readonly deployment: Option<BehaviorGroupDeployment>,
    readonly metaData: Option<BehaviorGroupMetaData>,
    readonly isManaged: boolean,
    readonly managedContact: Option<User>,
    readonly linkedGithubRepo: Option<LinkedGithubRepo>
  ) {
      Object.defineProperties(this, {
        id: { value: id, enumerable: true },
        teamId: { value: teamId, enumerable: true },
        name: { value: name, enumerable: true },
        icon: { value: icon, enumerable: true },
        description: { value: description, enumerable: true },
        actionInputs: { value: actionInputs, enumerable: true },
        dataTypeInputs: { value: dataTypeInputs, enumerable: true },
        behaviorVersions: { value: behaviorVersions, enumerable: true },
        libraryVersions: { value: libraryVersions, enumerable: true },
        requiredAWSConfigs: { value: requiredAWSConfigs, enumerable: true },
        requiredOAuth1ApiConfigs: { value: requiredOAuth1ApiConfigs, enumerable: true },
        requiredOAuth2ApiConfigs: { value: requiredOAuth2ApiConfigs, enumerable: true },
        requiredSimpleTokenApis: { value: requiredSimpleTokenApis, enumerable: true },
        createdAt: { value: createdAt, enumerable: true },
        exportId: { value: exportId, enumerable: true },
        author: { value: author, enumerable: true },
        gitSHA: { value: gitSHA, enumerable: true },
        deployment: { value: deployment, enumerable: true },
        metaData: { value: metaData, enumerable: true },
        isManaged: { value: isManaged, enumerable: true },
        managedContact: { value: managedContact, enumerable: true },
        linkedGithubRepo: { value: linkedGithubRepo, enumerable: true }
      });
  }

    getEditables(): ReadonlyArray<Editable> {
      const arr: Array<Editable> = [];
      return arr.concat(this.behaviorVersions, this.libraryVersions);
    }

    getRequiredAWSConfigs(): Array<RequiredAWSConfig> {
      return this.requiredAWSConfigs || [];
    }

    getRequiredOAuth1ApiConfigs(): Array<RequiredOAuth1Application> {
      return this.requiredOAuth1ApiConfigs || [];
    }

    getRequiredOAuth2ApiConfigs(): Array<RequiredOAuth2Application> {
      return this.requiredOAuth2ApiConfigs || [];
    }

    getRequiredSimpleTokenApis(): Array<RequiredSimpleTokenApi> {
      return this.requiredSimpleTokenApis || [];
    }

    needsConfig(): boolean {
      return (this.getRequiredOAuth2ApiConfigs().filter(ea => !ea.config).length > 0) ||
        (this.getRequiredOAuth1ApiConfigs().filter(ea => !ea.config).length > 0);
    }

    static timestampToNumber(t: Option<Timestamp>): Option<number> {
      if (typeof t === "number") {
        return t;
      } else if (typeof t === "string") {
        return Number(new Date(t));
      } else if (t instanceof Date) {
        return Number(t);
      } else {
        return null;
      }
    }

    isRecentlySaved(): boolean {
      const dateAsNumber = BehaviorGroup.timestampToNumber(this.createdAt);
      return Boolean(dateAsNumber && (dateAsNumber > (Number(new Date()) - ONE_MINUTE)));
    }

    isExisting(): boolean {
      return Boolean(this.id);
    }

    getName(): string {
      return this.name || this.getUntitledName();
    }

    getUntitledName(): string {
      return this.id ? "Untitled skill" : "New skill";
    }

    getDescription(): string {
      return this.description || "";
    }

    getActions(): Array<BehaviorVersion> {
      return this.behaviorVersions.filter(ea => !ea.isDataType() && !ea.isTest());
    }

    getDataTypes(): Array<BehaviorVersion> {
      return this.behaviorVersions.filter(ea => ea.isDataType());
    }

    getTests(): Array<BehaviorVersion> {
      return this.behaviorVersions.filter(ea => ea.isTest());
    }

    getInputs(): Array<Input> {
      return this.actionInputs.concat(this.dataTypeInputs);
    }

    getAllInputIdsFromBehaviorVersions(): Set<string> {
      let inputIds = new Set();
      this.behaviorVersions.forEach(ea => {
        ea.inputIds.forEach(eaId => inputIds.add(eaId));
      });
      return inputIds;
    }

    copyWithObsoleteInputsRemoved(): BehaviorGroup {
      const inputIdsUsed = this.getAllInputIdsFromBehaviorVersions();
      return this.clone({
        actionInputs: this.actionInputs.filter(ea => ea.inputId && inputIdsUsed.has(ea.inputId)),
        dataTypeInputs: this.dataTypeInputs.filter(ea => ea.inputId && inputIdsUsed.has(ea.inputId))
      });
    }

    clone(props: Partial<BehaviorGroupInterface>): BehaviorGroup {
      return BehaviorGroup.fromProps(Object.assign({}, this, props));
    }

    copyWithNewTimestamp(): BehaviorGroup {
      return this.clone({ createdAt: Date.now() });
    }

    copyWithInputsForBehaviorVersion(inputsForBehavior: Array<Input>, behaviorVersion: BehaviorVersion): BehaviorGroup {
      const inputIdsForBehavior = inputsForBehavior.map(ea => ea.inputId).filter((ea): ea is string => typeof ea === 'string');
      const newBehaviorVersion = behaviorVersion.clone({ inputIds: inputIdsForBehavior }).copyWithNewTimestamp();
      const inputs = behaviorVersion.isDataType() ? this.dataTypeInputs : this.actionInputs;
      const newInputs =
        inputs.
          filter(ea => ea.inputId && inputIdsForBehavior.indexOf(ea.inputId) === -1).
          concat(inputsForBehavior);
      const newBehaviorVersions =
        this.behaviorVersions.
          filter(ea => ea.behaviorId !== newBehaviorVersion.behaviorId).
          concat([newBehaviorVersion]);
      const newGroupProps: { behaviorVersions: Array<BehaviorVersion>, dataTypeInputs?: Array<Input>, actionInputs?: Array<Input> } = {
        behaviorVersions: newBehaviorVersions
      };
      if (behaviorVersion.isDataType()) {
        newGroupProps.dataTypeInputs = newInputs;
      } else {
        newGroupProps.actionInputs = newInputs;
      }
      return this.clone(newGroupProps).copyWithObsoleteInputsRemoved();
    }

    // Used by JSON.stringify for submitting data to the server
    toJSON() {
      return this.clone({
        behaviorVersions: this.sortedForComparison(this.behaviorVersions).map((ea) => ea.forEqualityComparison()),
        libraryVersions: this.sortedForComparison(this.libraryVersions).map((ea) => ea.forEqualityComparison()),
        createdAt: null,
        author: null,
        deployment: null
      });
    }

    forEqualityComparison() {
      return this.toJSON();
    }

    isIdenticalTo(group: BehaviorGroup): boolean {
      return DeepEqual.isEqual(this.forEqualityComparison(), group.forEqualityComparison());
    }

    isValidForDataStorage(): boolean {
      return this.getDataTypes().every(ea => {
        const config = ea.getDataTypeConfig();
        return Boolean(ea.name && ea.name.length > 0 && config && config.isValidForDataStorage());
      });
    }

    withNewBehaviorVersion(behaviorVersion: BehaviorVersion): BehaviorGroup {
      return this.clone({
        behaviorVersions: this.behaviorVersions.concat([behaviorVersion])
      });
    }

    withNewLibraryVersion(libraryVersion: LibraryVersion): BehaviorGroup {
      return this.clone({
        libraryVersions: this.libraryVersions.concat([libraryVersion])
      });
    }

    hasBehaviorVersionWithId(behaviorId: string): boolean {
      return !!this.behaviorVersions.find(ea => ea.behaviorId === behaviorId);
    }

    getCustomParamTypes(): Array<ParamType> {
      return this.behaviorVersions.
        filter(ea => ea.isDataType()).
        map(ea => ea.toParamType());
    }

    sortedForComparison<T extends Editable>(versions: Array<T>): Array<T> {
      return versions.sort((a, b) => {
        if (a.getPersistentId() < b.getPersistentId()) {
          return -1;
        } else if (a.getPersistentId() > b.getPersistentId()) {
          return 1;
        } else {
          return 0;
        }
      });
    }

    diffLabel(): string {
      const itemLabel = this.itemLabel();
      const kindLabel = this.kindLabel();
      return itemLabel ? `${kindLabel} “${itemLabel}”` : `untitled ${kindLabel}`;
    }

    itemLabel(): Option<string> {
      return this.getName();
    }

    kindLabel(): string {
      return "skill";
    }

    getIdForDiff(): string {
      return this.id || "unknown";
    }

    getInitialAuthor(): Option<User> {
      return this.metaData ? this.metaData.initialAuthor : null;
    }

    getInitialCreatedAt(): Option<Timestamp> {
      return this.metaData ? this.metaData.initialCreatedAt : null;
    }

    diffProps(): Array<DiffableProp> {
      return [{
        name: "Skill name",
        value: this.name || ""
      }, {
        name: "Skill description",
        value: this.getDescription()
      }, {
        name: "Skill icon",
        value: this.icon || ""
      }, {
        name: "Actions and data types",
        value: this.behaviorVersions,
        parent: this
      }, {
        name: "Libraries",
        value: this.libraryVersions
      }, {
        name: "Required AWS configurations",
        value: this.requiredAWSConfigs
      }, {
        name: "Required OAuth1 configurations",
        value: this.requiredOAuth1ApiConfigs
      }, {
        name: "Required OAuth2 configurations",
        value: this.requiredOAuth2ApiConfigs
      }, {
        name: "Required simple token API configurations",
        value: this.requiredSimpleTokenApis
      }];
    }

    static fromProps(props: BehaviorGroupInterface): BehaviorGroup {
      return new BehaviorGroup(
        props.id,
        props.teamId,
        props.name,
        props.icon,
        props.description,
        props.actionInputs,
        props.dataTypeInputs,
        props.behaviorVersions,
        props.libraryVersions,
        props.requiredAWSConfigs,
        props.requiredOAuth1ApiConfigs,
        props.requiredOAuth2ApiConfigs,
        props.requiredSimpleTokenApis,
        props.createdAt,
        props.exportId,
        props.author,
        props.gitSHA,
        props.deployment,
        props.metaData,
        props.isManaged,
        props.managedContact,
        props.linkedGithubRepo
      );
    }

    static fromJson(props: BehaviorGroupJson): BehaviorGroup {
      return BehaviorGroup.fromProps(Object.assign({}, props, {
        requiredAWSConfigs: props.requiredAWSConfigs.map(RequiredAWSConfig.fromJson),
        requiredOAuth1ApiConfigs: props.requiredOAuth1ApiConfigs.map(RequiredOAuth1Application.fromJson),
        requiredOAuth2ApiConfigs: props.requiredOAuth2ApiConfigs.map(RequiredOAuth2Application.fromJson),
        requiredSimpleTokenApis: props.requiredSimpleTokenApis.map(RequiredSimpleTokenApi.fromJson),
        behaviorVersions: props.behaviorVersions.map((ea) => BehaviorVersion.fromJson(Object.assign({}, ea, { groupId: props.id }))),
        actionInputs: Input.allFromJson(props.actionInputs || []),
        dataTypeInputs: Input.allFromJson(props.dataTypeInputs || []),
        libraryVersions: props.libraryVersions.map(ea => LibraryVersion.fromProps(Object.assign({}, ea, { groupId: props.id }))),
        author: props.author ? User.fromJson(props.author) : null,
        deployment: props.deployment ? BehaviorGroupDeployment.fromJson(props.deployment) : null,
        metaData: props.metaData ? BehaviorGroupMetaData.fromJson(props.metaData) : null,
        managedContact: props.managedContact ? User.fromJson(props.managedContact) : null,
        linkedGithubRepo: props.linkedGithubRepo ? LinkedGithubRepo.fromJson(props.linkedGithubRepo) : null
      }));
    }

    static groupsIncludeExportId(groups: Array<BehaviorGroup>, exportId: Option<string>): boolean {
      return Boolean(exportId && groups.some((ea) => ea.exportId === exportId));
    }
}

export default BehaviorGroup;
