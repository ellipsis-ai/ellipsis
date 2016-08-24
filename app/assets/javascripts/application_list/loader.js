/* global ApplicationListConfig:false */

requirejs(['../common'], function() {
  requirejs(['react', 'react-dom', './application_list/index'], function(React, ReactDOM, ApplicationList) {
    var config = ApplicationListConfig;
    var myApplicationList = React.createElement(ApplicationList, config);
    ReactDOM.render(myApplicationList, document.getElementById(config.containerId));
  });
});
