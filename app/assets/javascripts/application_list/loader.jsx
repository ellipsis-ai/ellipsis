requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './application_list/index', 'config/oauth2application/list', './aws_config_list/index', 'config/awsconfig/list', './shared_ui/page'],
  function(Core, Fetch, React, ReactDOM, ApplicationList, ApplicationListConfig, ConfigList, ConfigListConfig, Page) {
    ReactDOM.render(
      (
        <Page>
          <ApplicationList {...ApplicationListConfig} />
        </Page>
      ),
      document.getElementById(ApplicationListConfig.containerId)
    );
  });
});
