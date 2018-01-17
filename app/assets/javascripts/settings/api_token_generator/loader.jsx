requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './settings/api_token_generator/index', 'config/api/listTokens', './shared_ui/page'],
  function(Core, Fetch, React, ReactDOM, ApiTokenGenerator, ApiTokenGeneratorConfig, Page) {
    ReactDOM.render(
      (
        <Page csrfToken={ApiTokenGeneratorConfig.csrfToken}>
          <ApiTokenGenerator {...ApiTokenGeneratorConfig} />
        </Page>
      ),
      document.getElementById(ApiTokenGeneratorConfig.containerId)
    );
  });
});
