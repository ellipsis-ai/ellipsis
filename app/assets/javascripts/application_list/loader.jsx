requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './application_list/index', 'config/oauth2application/list'],
  function(Core, Fetch, React, ReactDOM, ApplicationList, ApplicationListConfig) {
    ReactDOM.render(
      React.createElement(ApplicationList, ApplicationListConfig),
      document.getElementById(ApplicationListConfig.containerId)
    );
  });
});
