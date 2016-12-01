/* global EnvironmentVariableListConfig:false */

requirejs(['../common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './environment_variables/index'],
    function(Core, Fetch, React, ReactDOM, EnvironmentVariableList) {
    var config = EnvironmentVariableListConfig;
    var myEnvironmentVariableList = React.createElement(EnvironmentVariableList, config);
    ReactDOM.render(myEnvironmentVariableList, document.getElementById(config.containerId));
  });
});
