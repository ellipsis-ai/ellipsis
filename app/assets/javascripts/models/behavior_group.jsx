define(function(require) {
  var BehaviorVersion = require('./behavior_version');
  var DeepEqual = require('../lib/deep_equal');

  class BehaviorGroup {
    constructor(props) {
      Object.assign(this, props);

      Object.defineProperties(this, {
        id: { value: props.id, enumerable: true },
        teamId: { value: props.teamId, enumerable: true },
        name: { value: props.name, enumerable: true },
        icon: { value: props.icon, enumerable: true },
        description: { value: props.description, enumerable: true },
        actionInputs: { value: props.actionInputs, enumerable: true },
        dataTypeInputs: { value: props.dataTypeInputs, enumerable: true },
        behaviorVersions: { value: props.behaviorVersions, enumerable: true },
        createdAt: { value: props.createdAt, enumerable: false },
        exportId: { value: props.exportId, enumerable: false }
      });
    }

    clone(props) {
      return new BehaviorGroup(Object.assign({}, this, props));
    }

    forEqualityComparison() {
      return this.clone({ behaviorVersions: this.sortedForComparison(this.behaviorVersions) });
    }

    isIdenticalTo(group) {
      return DeepEqual.isEqual(this.forEqualityComparison(), group.forEqualityComparison());
    }

    withNewBehaviorVersion(behaviorVersion) {
      return this.clone({
        behaviorVersions: this.behaviorVersions.concat([behaviorVersion])
      });
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
        behaviorVersions: props.behaviorVersions.map((ea) => BehaviorVersion.fromJson(ea))
      }));
    }
  }

  return BehaviorGroup;
});
