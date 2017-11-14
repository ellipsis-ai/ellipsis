requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './github_config/index', 'config/github/index', './shared_ui/page'],
    function(Core, Fetch, React, ReactDOM, GithubConfig, GithubConfigConfig, Page) {
      ReactDOM.render(
        (
          <Page csrfToken={GithubConfigConfig.csrfToken}>
            <GithubConfig {...GithubConfigConfig} />
          </Page>
        ),
        document.getElementById(GithubConfigConfig.containerId)
      );
    });
});
