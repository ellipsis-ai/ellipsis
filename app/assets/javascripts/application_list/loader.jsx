requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './application_list/index', 'config/oauth2application/list', './shared_ui/page'],
  function(Core, Fetch, React, ReactDOM, ApplicationList, ApplicationListConfig, Page) {
    ReactDOM.render(
      (
        <Page csrfToken={ApplicationListConfig.csrfToken}>
          <ApplicationList {...ApplicationListConfig} />
        </Page>
      ),
      document.getElementById(ApplicationListConfig.containerId)
    );
  });
});
