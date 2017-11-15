requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './settings/integrations/index', 'web/settings/integrations/list', './shared_ui/page'],
    function(Core, Fetch, React, ReactDOM, IntegrationList, IntegrationListConfig, Page) {
      ReactDOM.render(
        (
          <Page csrfToken={IntegrationListConfig.csrfToken}>
            <IntegrationList {...IntegrationListConfig} />
          </Page>
        ),
        document.getElementById(IntegrationListConfig.containerId)
      );
    });
});
