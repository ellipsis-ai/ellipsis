/* global ApiTokenGeneratorConfig:false */

requirejs(['../common'], function() {
  requirejs(['core-js', 'react', 'react-dom', './api_token_generator/index'], function(Core, React, ReactDOM, ApiTokenGenerator) {
    var config = ApiTokenGeneratorConfig;
    var myApiTokenGenerator = React.createElement(ApiTokenGenerator, config);
    ReactDOM.render(myApiTokenGenerator, document.getElementById(config.containerId));
  });
});
