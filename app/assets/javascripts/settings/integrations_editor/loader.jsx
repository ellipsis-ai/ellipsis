requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './settings/integrations_editor/index', 'pippo', './shared_ui/page'],
  function(Core, Fetch, React, ReactDOM, ApplicationEditor, ApplicationEditorConfig, Page) {
    ReactDOM.render(
      (
        <Page>
          <ApplicationEditor {...ApplicationEditorConfig} />
        </Page>
      ),
      document.getElementById(ApplicationEditorConfig.containerId)
    );
  });
});
