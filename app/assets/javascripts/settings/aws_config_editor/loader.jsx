requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './settings/aws_config_editor/index', 'config/awsconfig/edit', './shared_ui/page'],
    function(Core, Fetch, React, ReactDOM, AwsConfigEditor, AwsConfigEditorConfig, Page) {
      ReactDOM.render(
        (
          <Page csrfToken={AwsConfigEditorConfig.csrfToken}>
            <AwsConfigEditor {...AwsConfigEditorConfig} />
          </Page>
        ),
        document.getElementById(AwsConfigEditorConfig.containerId)
      );
    });
});
