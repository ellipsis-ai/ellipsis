requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './environment_variables/index', 'config/environmentvariables/list', './shared_ui/page'],
  function(Core, Fetch, React, ReactDOM, EnvironmentVariableList, EnvironmentVariableListConfig, Page) {
    ReactDOM.render(
      (
        <Page csrfToken={EnvironmentVariableListConfig.csrfToken}>
          <EnvironmentVariableList {...EnvironmentVariableListConfig} />
        </Page>
      ),
      document.getElementById(EnvironmentVariableListConfig.containerId)
    );
  });
});
