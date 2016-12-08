define(function(require) {
  var BehaviorVersion = require('./behavior_version');

  class BehaviorGroup {
    constructor(props) {
      Object.assign(this, props);
    }

    clone(props) {
      return new BehaviorGroup(Object.assign({}, this, props));
    }

    static fromJson(props) {
      return new BehaviorGroup(Object.assign({}, props, {
        behaviorVersions: props.behaviorVersions.map((ea) => BehaviorVersion.fromJson(ea))
      }));
    }
  }

  return BehaviorGroup;
});
