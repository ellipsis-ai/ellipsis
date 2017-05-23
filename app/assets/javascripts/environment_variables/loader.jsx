requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './environment_variables/index', 'config/environmentvariables/list'],
  function(Core, Fetch, React, ReactDOM, EnvironmentVariableList, EnvironmentVariableListConfig) {
    ReactDOM.render(
      React.createElement(EnvironmentVariableList, EnvironmentVariableListConfig),
      document.getElementById(EnvironmentVariableListConfig.containerId)
    );
  });
});
