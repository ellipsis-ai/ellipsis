/* global BehaviorListConfig:false */

requirejs(['../common'], function() {
  requirejs(
    ['core-js', 'react', 'react-dom', './behavior_list/app'],
    function(Core, React, ReactDOM, BehaviorListApp) {
      ReactDOM.render(
        React.createElement(BehaviorListApp, BehaviorListConfig),
        document.getElementById(BehaviorListConfig.containerId)
      );
    }
  );
});
