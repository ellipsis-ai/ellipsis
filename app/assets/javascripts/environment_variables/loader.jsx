/* global EnvironmentVariableListConfig:false */

requirejs(['../common'], function() {
  requirejs(['core-js', 'react', 'react-dom', './environment_variables/index'], function(Core, React, ReactDOM, EnvironmentVariableList) {
    var config = EnvironmentVariableListConfig;
    var myEnvironmentVariableList = React.createElement(EnvironmentVariableList, config);
    ReactDOM.render(myEnvironmentVariableList, document.getElementById(config.containerId));
  });
});
