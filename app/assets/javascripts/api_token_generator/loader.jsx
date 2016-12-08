/* global ApiTokenGeneratorConfig:false */

requirejs(['../common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './api_token_generator/index'],
  function(Core, Fetch, React, ReactDOM, ApiTokenGenerator) {
    var config = ApiTokenGeneratorConfig;
    var myApiTokenGenerator = React.createElement(ApiTokenGenerator, config);
    ReactDOM.render(myApiTokenGenerator, document.getElementById(config.containerId));
  });
});
