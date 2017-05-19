requirejs(['common'], function() {
  requirejs(
    ['core-js', 'whatwg-fetch', 'react', 'react-dom', './behavior_list/app', 'config/index'],
    function(Core, Fetch, React, ReactDOM, BehaviorListApp, BehaviorListConfig) {
      ReactDOM.render(
        React.createElement(BehaviorListApp, BehaviorListConfig),
        document.getElementById(BehaviorListConfig.containerId)
      );
    }
  );
});
