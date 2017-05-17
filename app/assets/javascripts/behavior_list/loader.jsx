/* global BehaviorListConfig:false */

requirejs(['../common'], function() {
  requirejs(
    ['core-js', 'whatwg-fetch', 'react', 'react-dom', './behavior_list/app'],
    function(Core, Fetch, React, ReactDOM, BehaviorListApp) {
      ReactDOM.render(
        React.createElement(BehaviorListApp, BehaviorListConfig),
        document.getElementById(BehaviorListConfig.containerId)
      );
    }
  );
});
