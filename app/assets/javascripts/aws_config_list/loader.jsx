requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './aws_config_list/index', 'config/awsconfig/list', './shared_ui/page'],
   function(Core, Fetch, React, ReactDOM, ConfigList, ConfigListConfig, Page) {
      ReactDOM.render(
        (
          <Page csrfToken={ConfigListConfig.csrfToken}>
            <ConfigList {...ConfigListConfig} />
          </Page>
        ),
        document.getElementById(ConfigListConfig.containerId)
      );
    });
});
