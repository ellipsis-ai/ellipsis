import {Diffable, DiffableProp} from './diffs';

import BehaviorGroupDeployment, {BehaviorGroupDeploymentJson} from './behavior_group_deployment';
import BehaviorGroupMetaData, {BehaviorGroupMetaDataJson} from "./behavior_group_meta_data";
import BehaviorVersion, {BehaviorVersionJson} from './behavior_version';
import Editable from './editable';
import LibraryVersion, {LibraryVersionJson} from './library_version';
import Input, {InputJson} from './input';
import DeepEqual from '../lib/deep_equal';
import {RequiredAWSConfig, RequiredAWSConfigJson} from './aws';
import {RequiredOAuth2Application, RequiredOAuth2ApplicationJson} from './oauth2';
import {RequiredSimpleTokenApi, RequiredSimpleTokenApiJson} from './simple_token';
import User, {UserJson} from './user';
import ParamType from "./param_type";

const ONE_MINUTE = 60000;

export interface BehaviorGroupJson {
  id: string | null;
  teamId: string;
  name: string | null;
  icon: string | null;
  description: string | null;
  githubUrl: string | null;
  actionInputs: Array<InputJson>;
  dataTypeInputs: Array<InputJson>;
  behaviorVersions: Array<BehaviorVersionJson>;
  libraryVersions: Array<LibraryVersionJson>;
  requiredAWSConfigs: Array<RequiredAWSConfigJson>;
  requiredOAuth2ApiConfigs: Array<RequiredOAuth2ApplicationJson>;
  requiredSimpleTokenApis: Array<RequiredSimpleTokenApiJson>;
  createdAt: number | null;
  exportId: string | null;
  author: UserJson | null;
  gitSHA: string | null;
  deployment: BehaviorGroupDeploymentJson | null;
  metaData: BehaviorGroupMetaDataJson | null;
}

interface BehaviorGroupInterface extends BehaviorGroupJson {
  actionInputs: Array<Input>;
  dataTypeInputs: Array<Input>;
  behaviorVersions: Array<BehaviorVersion>;
  libraryVersions: Array<LibraryVersion>;
  requiredAWSConfigs: Array<RequiredAWSConfig>;
  requiredOAuth2ApiConfigs: Array<RequiredOAuth2Application>;
  requiredSimpleTokenApis: Array<RequiredSimpleTokenApi>;
  author: User | null;
  deployment: BehaviorGroupDeployment | null;
  metaData: BehaviorGroupMetaData | null;
}

class BehaviorGroup implements Diffable, BehaviorGroupInterface {
  constructor(
    readonly id: string | null,
    readonly teamId: string,
    readonly name: string | null,
    readonly icon: string | null,
    readonly description: string | null,
    readonly githubUrl: string | null,
    readonly actionInputs: Array<Input>,
    readonly dataTypeInputs: Array<Input>,
    readonly behaviorVersions: Array<BehaviorVersion>,
    readonly libraryVersions: Array<LibraryVersion>,
    readonly requiredAWSConfigs: Array<RequiredAWSConfig>,
    readonly requiredOAuth2ApiConfigs: Array<RequiredOAuth2Application>,
    readonly requiredSimpleTokenApis: Array<RequiredSimpleTokenApi>,
    readonly createdAt: number | null,
    readonly exportId: string | null,
    readonly author: User | null,
    readonly gitSHA: string | null,
    readonly deployment: BehaviorGroupDeployment | null,
    readonly metaData: BehaviorGroupMetaData | null
  ) {
      Object.defineProperties(this, {
        id: { value: id, enumerable: true },
        teamId: { value: teamId, enumerable: true },
        name: { value: name, enumerable: true },
        icon: { value: icon, enumerable: true },
        description: { value: description, enumerable: true },
        githubUrl: { value: githubUrl, enumerable: true },
        actionInputs: { value: actionInputs, enumerable: true },
        dataTypeInputs: { value: dataTypeInputs, enumerable: true },
        behaviorVersions: { value: behaviorVersions, enumerable: true },
        libraryVersions: { value: libraryVersions, enumerable: true },
        requiredAWSConfigs: { value: requiredAWSConfigs, enumerable: true },
        requiredOAuth2ApiConfigs: { value: requiredOAuth2ApiConfigs, enumerable: true },
        requiredSimpleTokenApis: { value: requiredSimpleTokenApis, enumerable: true },
        createdAt: { value: createdAt, enumerable: true },
        exportId: { value: exportId, enumerable: true },
        author: { value: author, enumerable: true },
        gitSHA: { value: gitSHA, enumerable: true },
        deployment: { value: deployment, enumerable: true },
        metaData: { value: metaData, enumerable: true }
      });
  }

    getEditables(): ReadonlyArray<Editable> {
      const arr: Array<Editable> = [];
      return arr.concat(this.behaviorVersions, this.libraryVersions);
    }

    getRequiredAWSConfigs(): Array<RequiredAWSConfig> {
      return this.requiredAWSConfigs || [];
    }

    getRequiredOAuth2ApiConfigs(): Array<RequiredOAuth2Application> {
      return this.requiredOAuth2ApiConfigs || [];
    }

    getRequiredSimpleTokenApis(): Array<RequiredSimpleTokenApi> {
      return this.requiredSimpleTokenApis || [];
    }

    needsConfig(): boolean {
      return this.getRequiredOAuth2ApiConfigs().filter(ea => !ea.config).length > 0;
    }

    isRecentlySaved(): boolean {
      return !!this.createdAt && Number(new Date(this.createdAt)) > Number(new Date()) - ONE_MINUTE;
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
      return this.behaviorVersions.filter(ea => !ea.isDataType());
    }

    getDataTypes(): Array<BehaviorVersion> {
      return this.behaviorVersions.filter(ea => ea.isDataType());
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
        actionInputs: this.actionInputs.filter(ea => inputIdsUsed.has(ea.inputId)),
        dataTypeInputs: this.dataTypeInputs.filter(ea => inputIdsUsed.has(ea.inputId))
      });
    }

    clone(props: Partial<BehaviorGroupInterface>): BehaviorGroup {
      return BehaviorGroup.fromProps(Object.assign({}, this, props));
    }

    copyWithNewTimestamp(): BehaviorGroup {
      return this.clone({ createdAt: Date.now() });
    }

    copyWithInputsForBehaviorVersion(inputsForBehavior: Array<Input>, behaviorVersion: BehaviorVersion): BehaviorGroup {
      const inputIdsForBehavior = inputsForBehavior.map(ea => ea.inputId);
      const newBehaviorVersion = behaviorVersion.clone({ inputIds: inputIdsForBehavior }).copyWithNewTimestamp();
      const inputs = behaviorVersion.isDataType() ? this.dataTypeInputs : this.actionInputs;
      const newInputs =
        inputs.
          filter(ea => inputIdsForBehavior.indexOf(ea.inputId) === -1).
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

    itemLabel(): string | null {
      return this.getName();
    }

    kindLabel(): string {
      return "skill";
    }

    getIdForDiff(): string {
      return this.id || "unknown";
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
        props.githubUrl,
        props.actionInputs,
        props.dataTypeInputs,
        props.behaviorVersions,
        props.libraryVersions,
        props.requiredAWSConfigs,
        props.requiredOAuth2ApiConfigs,
        props.requiredSimpleTokenApis,
        props.createdAt,
        props.exportId,
        props.author,
        props.gitSHA,
        props.deployment,
        props.metaData
      );
    }

    static fromJson(props: BehaviorGroupJson): BehaviorGroup {
      return BehaviorGroup.fromProps(Object.assign({}, props, {
        requiredAWSConfigs: props.requiredAWSConfigs.map(RequiredAWSConfig.fromJson),
        requiredOAuth2ApiConfigs: props.requiredOAuth2ApiConfigs.map(RequiredOAuth2Application.fromJson),
        requiredSimpleTokenApis: props.requiredSimpleTokenApis.map(RequiredSimpleTokenApi.fromJson),
        behaviorVersions: props.behaviorVersions.map((ea) => BehaviorVersion.fromJson(Object.assign({}, ea, { groupId: props.id }))),
        actionInputs: Input.allFromJson(props.actionInputs || []),
        dataTypeInputs: Input.allFromJson(props.dataTypeInputs || []),
        libraryVersions: props.libraryVersions.map(ea => LibraryVersion.fromProps(Object.assign({}, ea, { groupId: props.id }))),
        author: props.author ? User.fromJson(props.author) : null,
        deployment: props.deployment ? BehaviorGroupDeployment.fromJson(props.deployment) : null,
        metaData: props.metaData ? BehaviorGroupMetaData.fromJson(props.metaData) : null
      }));
    }

    static groupsIncludeExportId(groups: Array<BehaviorGroup>, exportId: string | null): boolean {
      return Boolean(exportId && groups.some((ea) => ea.exportId === exportId));
    }
}

export default BehaviorGroup;
