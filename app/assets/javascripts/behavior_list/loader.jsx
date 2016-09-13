/* global BehaviorListConfig:false */

requirejs(['../common'], function() {
  requirejs(['react', 'react-dom', './behavior_list/index'], function(React, ReactDOM, BehaviorList) {
    var config = BehaviorListConfig;
    var myBehaviorList = React.createElement(BehaviorList, config);
    ReactDOM.render(myBehaviorList, document.getElementById(config.containerId));
  });
});
