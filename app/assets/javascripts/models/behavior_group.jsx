// @flow

import type {Diffable, HasInputs, DiffableProp} from './diffs';

define(function(require) {
  const BehaviorGroupDeployment = require('./behavior_group_deployment');
  const BehaviorVersion = require('./behavior_version');
  const Editable = require('./editable');
  const LibraryVersion = require('./library_version');
  const Input = require('./input');
  const DeepEqual = require('../lib/deep_equal');
  const RequiredAWSConfig = require('./aws').RequiredAWSConfig;
  const RequiredOAuth2Application = require('./oauth2').RequiredOAuth2Application;
  const RequiredSimpleTokenApi = require('./simple_token').RequiredSimpleTokenApi;
  const User = require('./user');

  const ONE_MINUTE = 60000;

  class BehaviorGroup implements Diffable, HasInputs {
    id: string;
    teamId: string;
    name: ?string;
    icon: ?string;
    description: ?string;
    githubUrl: ?string;
    actionInputs: Array<Input>;
    dataTypeInputs: Array<Input>;
    behaviorVersions: Array<BehaviorVersion>;
    libraryVersions: Array<LibraryVersion>;
    requiredAWSConfigs: Array<RequiredAWSConfig>;
    requiredOAuth2ApiConfigs: Array<RequiredOAuth2Application>;
    requiredSimpleTokenApis: Array<RequiredSimpleTokenApi>;
    createdAt: ?number;
    exportId: ?string;
    author: ?User;
    gitSHA: ?string;
    deployment: ?BehaviorGroupDeployment;

    constructor(
      id: string,
      teamId: string,
      name: ?string,
      icon: ?string,
      description: ?string,
      githubUrl: ?string,
      actionInputs: Array<Input>,
      dataTypeInputs: Array<Input>,
      behaviorVersions: Array<BehaviorVersion>,
      libraryVersions: Array<LibraryVersion>,
      requiredAWSConfigs: Array<RequiredAWSConfig>,
      requiredOAuth2ApiConfigs: Array<RequiredOAuth2Application>,
      requiredSimpleTokenApis: Array<RequiredSimpleTokenApi>,
      createdAt: ?number,
      exportId: ?string,
      author: ?User,
      gitSHA: ?string,
      deployment: ?BehaviorGroupDeployment
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
        deployment: { value: deployment, enumerable: true }
      });
    }

    getEditables(): Array<Editable> {
      return this.behaviorVersions.concat(this.libraryVersions);
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
      return !!this.createdAt && new Date(this.createdAt) > (new Date() - ONE_MINUTE);
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

    clone(props): BehaviorGroup {
      return BehaviorGroup.fromProps(Object.assign({}, this, props));
    }

    copyWithNewTimestamp(): BehaviorGroup {
      return this.clone({ createdAt: Date.now() });
    }

    copyWithInputsForBehaviorVersion(inputsForBehavior, behaviorVersion): BehaviorGroup {
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
        behaviorVersions: this.sortedForComparison(this.behaviorVersions).map(BehaviorVersion.forEqualityComparison),
        libraryVersions: this.sortedForComparison(this.libraryVersions).map(LibraryVersion.forEqualityComparison),
        createdAt: null,
        author: null,
        deployment: null
      });
    }

    forEqualityComparison() {
      return this.toJSON();
    }

    isIdenticalTo(group): boolean {
      return DeepEqual.isEqual(this.forEqualityComparison(), group.forEqualityComparison());
    }

    isValidForDataStorage(): boolean {
      return this.getDataTypes().every(ea => {
        return ea.name && ea.name.length > 0 && ea.getDataTypeConfig().isValidForDataStorage();
      });
    }

    withNewBehaviorVersion(behaviorVersion): BehaviorGroup {
      return this.clone({
        behaviorVersions: this.behaviorVersions.concat([behaviorVersion])
      });
    }

    withNewLibraryVersion(libraryVersion): BehaviorGroup {
      return this.clone({
        libraryVersions: this.libraryVersions.concat([libraryVersion])
      });
    }

    hasBehaviorVersionWithId(behaviorId): boolean {
      return !!this.behaviorVersions.find(ea => ea.behaviorId === behaviorId);
    }

    getCustomParamTypes(): Array<BehaviorVersion> {
      return this.behaviorVersions.
        filter(ea => ea.isDataType()).
        map(ea => ea.toParamType());
    }

    sortedForComparison(versions): Array<BehaviorVersion> {
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

    itemLabel(): ?string {
      return this.getName();
    }

    kindLabel(): string {
      return "skill";
    }

    getIdForDiff(): string {
      return this.id;
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

    static fromProps(props): BehaviorGroup {
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
        props.deployment
      );
    }

    static fromJson(props): BehaviorGroup {
      return BehaviorGroup.fromProps(Object.assign({}, props, {
        requiredAWSConfigs: props.requiredAWSConfigs.map(RequiredAWSConfig.fromJson),
        requiredOAuth2ApiConfigs: props.requiredOAuth2ApiConfigs.map(RequiredOAuth2Application.fromJson),
        requiredSimpleTokenApis: props.requiredSimpleTokenApis.map(RequiredSimpleTokenApi.fromJson),
        behaviorVersions: props.behaviorVersions.map((ea) => BehaviorVersion.fromJson(Object.assign({}, ea, { groupId: props.id }))),
        actionInputs: Input.allFromJson(props.actionInputs || []),
        dataTypeInputs: Input.allFromJson(props.dataTypeInputs || []),
        libraryVersions: props.libraryVersions.map(ea => LibraryVersion.fromProps(Object.assign({}, ea, { groupId: props.id }))),
        author: props.author ? User.fromJson(props.author) : null,
        deployment: props.deployment ? BehaviorGroupDeployment.fromProps(props.deployment) : null
      }));
    }

    static groupsIncludeExportId(groups, exportId): Array<BehaviorGroup> {
      return groups.some((ea) => ea.exportId === exportId);
    }
  }

  return BehaviorGroup;
});
