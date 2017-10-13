requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './settings/integrations/index', 'web/settings/integrations/list', './shared_ui/page'],
    function(Core, Fetch, React, ReactDOM, ApplicationList, ApplicationListConfig, Page) {
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
