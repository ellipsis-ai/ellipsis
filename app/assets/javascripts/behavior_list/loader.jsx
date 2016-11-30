/* global BehaviorListConfig:false */

requirejs(['../common'], function() {
  requirejs(
    ['react', 'react-dom', './behavior_list/index', './models/behavior_group'],
    function(React, ReactDOM, BehaviorList, BehaviorGroup) {
      var config = BehaviorListConfig;
      var behaviorListProps = Object.assign({}, config, {
        behaviorGroups: config.behaviorGroups.map((ea) => BehaviorGroup.fromJson(ea))
      });
      var myBehaviorList = React.createElement(BehaviorList, behaviorListProps);
      ReactDOM.render(myBehaviorList, document.getElementById(config.containerId));
    }
  );
});
