/* global BehaviorListConfig:false */

requirejs(['../common'], function() {
  requirejs(
    ['core-js', 'whatwg-fetch', 'react', 'react-dom', './behavior_list/index', './models/behavior_group'],
    function(Core, Fetch, React, ReactDOM, BehaviorList, BehaviorGroup) {

      const defaultProps = Object.assign({
        onLoadPublishedBehaviorGroups: loadPublishedBehaviorGroups,
        onBehaviorGroupImport: importBehaviorGroup,
        publishedBehaviorGroupLoadStatus: 'loading',
        publishedBehaviorGroups: [],
        recentlyInstalled: []
      }, BehaviorListConfig);
      let recentProps = {};
      let recentlyInstalled = [];

      function loadPublishedBehaviorGroups() {
        fetch(jsRoutes.controllers.ApplicationController.fetchPublishedBehaviorInfo(defaultProps.teamId, defaultProps.branchName).url, {
          credentials: 'same-origin'
        }).then((response) => response.json())
          .then((json) => {
            reload({
              publishedBehaviorGroups: json,
              publishedBehaviorGroupLoadStatus: 'loaded'
            });
          }).catch(() => {
            reload({
              publishedBehaviorGroupLoadStatus: 'error'
            });
          });
      }

      function importBehaviorGroup(groupToInstall) {
        var headers = new Headers();
        headers.append('x-requested-with', 'XMLHttpRequest');
        var body = new FormData();
        body.append('csrfToken', recentProps.csrfToken);
        body.append('teamId', recentProps.teamId);
        body.append('dataJson', JSON.stringify(groupToInstall));
        fetch(jsRoutes.controllers.BehaviorImportExportController.doImport().url, {
          credentials: 'same-origin',
          headers: headers,
          method: 'POST',
          body: body
        }).then((response) => response.json())
          .then((installedGroup) => {
            recentlyInstalled = recentlyInstalled.concat(installedGroup);
            reload({
              behaviorGroups: BehaviorListConfig.behaviorGroups.concat(recentlyInstalled),
              recentlyInstalled: recentlyInstalled
            });
          });
      }

      function reload(newProps) {
        const behaviorListProps = Object.assign({}, defaultProps, recentProps, newProps);
        recentProps = Object.assign({}, behaviorListProps);

        behaviorListProps.behaviorGroups = behaviorListProps.behaviorGroups.map(BehaviorGroup.fromJson);
        behaviorListProps.publishedBehaviorGroups = behaviorListProps.publishedBehaviorGroups.map(BehaviorGroup.fromJson);
        behaviorListProps.recentlyInstalled = behaviorListProps.recentlyInstalled.map(BehaviorGroup.fromJson);

        ReactDOM.render(
          React.createElement(BehaviorList, behaviorListProps),
          document.getElementById(behaviorListProps.containerId)
        );
      }

      reload();
      loadPublishedBehaviorGroups();

    }
  );
});
