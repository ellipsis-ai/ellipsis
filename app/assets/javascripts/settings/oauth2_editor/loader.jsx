requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './settings/oauth2_editor/index', 'settings/integrations/editor', './shared_ui/page'],
  function(Core, Fetch, React, ReactDOM, IntegrationEditor, IntegrationEditorConfig, Page) {
    ReactDOM.render(
      (
        <Page csrfToken={IntegrationEditorConfig.csrfToken}>
          <IntegrationEditor {...IntegrationEditorConfig} />
        </Page>
      ),
      document.getElementById(IntegrationEditorConfig.containerId)
    );
  });
});
