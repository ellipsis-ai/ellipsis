/* global BehaviorListConfig:false */

requirejs(['../common'], function() {
  requirejs(
    ['react', 'react-dom', './behavior_list/index', './models/behavior_version'],
    function(React, ReactDOM, BehaviorList, BehaviorVersion) {
      var config = BehaviorListConfig;
      var behaviorListProps = Object.assign({}, config, {
        behaviorVersions: config.behaviorVersions.map((ea) => BehaviorVersion.fromJson(ea))
      });
      var myBehaviorList = React.createElement(BehaviorList, behaviorListProps);
      ReactDOM.render(myBehaviorList, document.getElementById(config.containerId));
    }
  );
});
