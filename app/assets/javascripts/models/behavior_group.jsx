define(function(require) {
  var BehaviorVersion = require('./behavior_version');
  var DeepEqual = require('../lib/deep_equal');

  class BehaviorGroup {
    constructor(props) {
      Object.defineProperties(this, {
        id: { value: props.id, enumerable: true },
        teamId: { value: props.teamId, enumerable: true },
        name: { value: props.name, enumerable: true },
        icon: { value: props.icon, enumerable: true },
        description: { value: props.description, enumerable: true },
        actionInputs: { value: props.actionInputs, enumerable: true },
        dataTypeInputs: { value: props.dataTypeInputs, enumerable: true },
        behaviorVersions: { value: props.behaviorVersions, enumerable: true },
        createdAt: { value: props.createdAt, enumerable: true },
        exportId: { value: props.exportId, enumerable: true }
      });
    }

    getName() {
      return this.name || "Untitled skill";
    }

    getActions() {
      return this.behaviorVersions.filter(ea => !ea.isDataType());
    }

    getDataTypes() {
      return this.behaviorVersions.filter(ea => ea.isDataType());
    }

    clone(props) {
      return new BehaviorGroup(Object.assign({}, this, props));
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
        behaviorVersions: props.behaviorVersions.map((ea) => BehaviorVersion.fromJson(Object.assign({}, ea, { groupId: props.id })))
      }));
    }
  }

  return BehaviorGroup;
});
