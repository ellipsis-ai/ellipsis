requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './aws_config_list/index', 'config/awsconfig/list'],
    function(Core, Fetch, React, ReactDOM, ConfigList, ConfigListConfig) {
      ReactDOM.render(
        React.createElement(ConfigList, ConfigListConfig),
        document.getElementById(ConfigListConfig.containerId)
      );
    });
});
