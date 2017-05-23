requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './api_token_generator/index', 'config/api/listTokens'],
  function(Core, Fetch, React, ReactDOM, ApiTokenGenerator, ApiTokenGeneratorConfig) {
    ReactDOM.render(
      React.createElement(ApiTokenGenerator, ApiTokenGeneratorConfig),
      document.getElementById(ApiTokenGeneratorConfig.containerId)
    );
  });
});
