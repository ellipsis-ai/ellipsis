define(function(require) {
  var BehaviorVersion = require('./behavior_version');

  class BehaviorGroup {
    constructor(props) {
      Object.assign(this, props);
    }

    clone(props) {
      return new BehaviorGroup(Object.assign({}, this, props));
    }

    withNewBehaviorData(behaviorProps) {
      var newVersion = BehaviorVersion.fromJson(behaviorProps);
      var updatedVersions =
        this.behaviorVersions.
          filter(ea => !!ea.behaviorId).
          filter(ea => ea.behaviorId !== newVersion.behaviorId).
          concat([newVersion]);
      return this.clone({ behaviorVersions: updatedVersions, id: newVersion.groupId });
    }

    withNewAction() {
      var newVersion = BehaviorVersion.fromJson({
        groupId: this.id,
        teamId: this.teamId
      });
      return this.withNewBehaviorVersion(newVersion);
    }

    withNewDataType() {
      var newVersion = BehaviorVersion.fromJson({
        config: { dataTypeName: "" },
        groupId: this.id,
        teamId: this.teamId
      });
      return this.withNewBehaviorVersion(newVersion);
    }

    withNewBehaviorVersion(behaviorVersion) {
      return this.clone({
        behaviorVersions: this.behaviorVersions.concat([behaviorVersion])
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
