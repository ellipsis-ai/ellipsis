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
      var updatedVersions = this.behaviorVersions.filter(ea => ea.behaviorId !== newVersion.behaviorId).concat([newVersion]);
      return this.clone({ behaviorVersions: updatedVersions, id: newVersion.groupId });
    }

    static fromJson(props) {
      return new BehaviorGroup(Object.assign({}, props, {
        behaviorVersions: props.behaviorVersions.map((ea) => BehaviorVersion.fromJson(ea))
      }));
    }
  }

  return BehaviorGroup;
});
