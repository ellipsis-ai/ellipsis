requirejs(['common'], function() {
  requirejs(
    ['core-js', 'whatwg-fetch', 'react', 'react-dom', './behavior_list/app', 'config/index', './shared_ui/page'],
    function(Core, Fetch, React, ReactDOM, BehaviorListApp, BehaviorListConfig, Page) {
      ReactDOM.render(
        (
          <Page>
            <BehaviorListApp {...BehaviorListConfig} />
          </Page>
        ),
        document.getElementById(BehaviorListConfig.containerId)
      );
    }
  );
});
