requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './aws_config_editor/index', 'config/awsconfig/edit', './shared_ui/page'],
    function(Core, Fetch, React, ReactDOM, ConfigEditor, ConfigEditorConfig, Page) {
      ReactDOM.render(
        (
          <Page>
            <ConfigEditor {...ConfigEditorConfig} />
          </Page>
        ),
        document.getElementById(ConfigEditorConfig.containerId)
      );
    });
});
