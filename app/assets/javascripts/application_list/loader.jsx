/* global ApplicationListConfig:false */

requirejs(['../common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './application_list/index'],
    function(Core, Fetch, React, ReactDOM, ApplicationList) {
    var config = ApplicationListConfig;
    var myApplicationList = React.createElement(ApplicationList, config);
    ReactDOM.render(myApplicationList, document.getElementById(config.containerId));
  });
});
