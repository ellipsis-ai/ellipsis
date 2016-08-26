/* global ApiTokenGeneratorConfig:false */

requirejs(['../common'], function() {
  requirejs(['react', 'react-dom', './api_token_generator/index'], function(React, ReactDOM, ApiTokenGenerator) {
    var config = ApiTokenGeneratorConfig;
    var myApiTokenGenerator = React.createElement(ApiTokenGenerator, config);
    ReactDOM.render(myApiTokenGenerator, document.getElementById(config.containerId));
  });
});
