define(function(require) {
  var BehaviorVersion = require('./behavior_version');
  var Input = require('./input');
  var DeepEqual = require('../lib/deep_equal');
  const ONE_MINUTE = 60000;

  class BehaviorGroup {
    constructor(props) {
      Object.defineProperties(this, {
        id: { value: props.id, enumerable: true },
        teamId: { value: props.teamId, enumerable: true },
        name: { value: props.name, enumerable: true },
        icon: { value: props.icon, enumerable: true },
        description: { value: props.description, enumerable: true },
        githubUrl: { value: props.githubUrl, enumerable: true },
        actionInputs: { value: props.actionInputs, enumerable: true },
        dataTypeInputs: { value: props.dataTypeInputs, enumerable: true },
        behaviorVersions: { value: props.behaviorVersions, enumerable: true },
        createdAt: { value: props.createdAt, enumerable: true },
        exportId: { value: props.exportId, enumerable: true }
      });
    }

    isRecentlySaved() {
      return !!this.createdAt && new Date(this.createdAt) > (new Date() - ONE_MINUTE);
    }

    getName() {
      return this.name || "Untitled skill";
    }

    getDescription() {
      return this.description || "";
    }

    getActions() {
      return this.behaviorVersions.filter(ea => !ea.isDataType());
    }

    getDataTypes() {
      return this.behaviorVersions.filter(ea => ea.isDataType());
    }

    getInputs() {
      return this.actionInputs.concat(this.dataTypeInputs);
    }

    getAllInputIdsFromBehaviorVersions() {
      let inputIds = new Set();
      this.behaviorVersions.forEach(ea => {
        ea.inputIds.forEach(eaId => inputIds.add(eaId));
      });
      return inputIds;
    }

    copyWithObsoleteInputsRemoved() {
      const inputIdsUsed = this.getAllInputIdsFromBehaviorVersions();
      return this.clone({
        actionInputs: this.actionInputs.filter(ea => inputIdsUsed.has(ea.inputId)),
        dataTypeInputs: this.dataTypeInputs.filter(ea => inputIdsUsed.has(ea.inputId))
      });
    }

    clone(props) {
      return new BehaviorGroup(Object.assign({}, this, props));
    }

    copyWithNewTimestamp() {
      return this.clone({ createdAt: Date.now() });
    }

    copyWithInputsForBehaviorVersion(inputsForBehavior, behaviorVersion) {
      const inputsKey = behaviorVersion.isDataType() ? "dataTypeInputs" : "actionInputs";
      const inputIdsForBehavior = inputsForBehavior.map(ea => ea.inputId);
      const newBehaviorVersion = behaviorVersion.clone({ inputIds: inputIdsForBehavior }).copyWithNewTimestamp();
      const newInputs =
        this[inputsKey].
          filter(ea => inputIdsForBehavior.indexOf(ea.inputId) === -1).
          concat(inputsForBehavior);
      const newBehaviorVersions =
        this.behaviorVersions.
          filter(ea => ea.behaviorId !== newBehaviorVersion.behaviorId).
          concat([newBehaviorVersion]);
      const newGroupProps = {
        behaviorVersions: newBehaviorVersions
      };
      newGroupProps[inputsKey] = newInputs;
      return this.clone(newGroupProps).copyWithObsoleteInputsRemoved();
    }

    // Used by JSON.stringify for submitting data to the server
    toJSON() {
      return this.clone({
        behaviorVersions: this.sortedForComparison(this.behaviorVersions).map((ea) => ea.forEqualityComparison()),
        createdAt: null
      });
    }

    forEqualityComparison() {
      return this.toJSON();
    }

    isIdenticalTo(group) {
      return DeepEqual.isEqual(this.forEqualityComparison(), group.forEqualityComparison());
    }

    withNewBehaviorVersion(behaviorVersion) {
      return this.clone({
        behaviorVersions: this.behaviorVersions.concat([behaviorVersion])
      });
    }

    hasBehaviorVersionWithId(behaviorId) {
      return !!this.behaviorVersions.find(ea => ea.behaviorId === behaviorId);
    }

    getCustomParamTypes() {
      return this.behaviorVersions.
        filter(ea => ea.isDataType()).
        map(ea => ea.toParamType());
    }

    sortedForComparison(versions) {
      return versions.sort((a, b) => {
        if (a.behaviorId < b.behaviorId) {
          return -1;
        } else if (a.behaviorId > b.behaviorId) {
          return 1;
        } else {
          return 0;
        }
      });
    }

    static fromJson(props) {
      return new BehaviorGroup(Object.assign({}, props, {
        behaviorVersions: props.behaviorVersions.map((ea) => BehaviorVersion.fromJson(Object.assign({}, ea, { groupId: props.id }))),
        actionInputs: Input.allFromJson(props.actionInputs || []),
        dataTypeInputs: Input.allFromJson(props.dataTypeInputs || [])
      }));
    }

    static groupsIncludeId(groups, groupId) {
      return groups.some((ea) => ea.id === groupId);
    }

    static groupsIncludeExportId(groups, exportId) {
      return groups.some((ea) => ea.exportId === exportId);
    }
  }

  return BehaviorGroup;
});
