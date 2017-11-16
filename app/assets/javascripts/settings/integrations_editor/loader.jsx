requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './settings/integrations_editor/index', 'settings/integrations/editor', './shared_ui/page'],
  function(Core, Fetch, React, ReactDOM, IntegrationEditor, IntegrationEditorConfig, Page) {
    ReactDOM.render(
      (
        <Page>
          <IntegrationEditor {...IntegrationEditorConfig} />
        </Page>
      ),
      document.getElementById(IntegrationEditorConfig.containerId)
    );
  });
});
