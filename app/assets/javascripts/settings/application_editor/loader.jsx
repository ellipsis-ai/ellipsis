requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './settings/application_editor/index', 'config/oauth2application/edit', './shared_ui/page'],
  function(Core, Fetch, React, ReactDOM, ApplicationEditor, ApplicationEditorConfig, Page) {
    ReactDOM.render(
      (
        <Page csrfToken={ApplicationEditorConfig.csrfToken}>
          <ApplicationEditor {...ApplicationEditorConfig} />
        </Page>
      ),
      document.getElementById(ApplicationEditorConfig.containerId)
    );
  });
});
