/* global BehaviorListConfig:false */

requirejs(['../common'], function() {
  requirejs(
    ['core-js', 'whatwg-fetch', 'react', 'react-dom', './behavior_list/index', './models/behavior_group'],
    function(Core, Fetch, React, ReactDOM, BehaviorList, BehaviorGroup) {

      class BehaviorListLoader {
        constructor(defaultProps) {
          this.defaultProps = Object.assign({
            onLoadPublishedBehaviorGroups: this.loadPublishedBehaviorGroups.bind(this),
            onBehaviorGroupImport: this.importBehaviorGroup.bind(this),
            publishedBehaviorGroupLoadStatus: 'loading',
            publishedBehaviorGroups: [],
            recentlyInstalled: []
          }, defaultProps);
          this.recentProps = {};
          this.recentlyInstalled = [];
        }

        loadPublishedBehaviorGroups() {
          var url = jsRoutes.controllers.ApplicationController.fetchPublishedBehaviorInfo(
            this.defaultProps.teamId, this.defaultProps.branchName
          ).url;
          fetch(url, {
            credentials: 'same-origin'
          }).then((response) => response.json())
            .then((json) => {
              this.reload({
                publishedBehaviorGroups: json,
                publishedBehaviorGroupLoadStatus: 'loaded'
              });
            }).catch(() => {
            this.reload({
              publishedBehaviorGroupLoadStatus: 'error'
            });
          });
        }

        importBehaviorGroup(groupToInstall) {
          var headers = new Headers();
          headers.append('x-requested-with', 'XMLHttpRequest');
          var body = new FormData();
          body.append('csrfToken', this.recentProps.csrfToken);
          body.append('teamId', this.recentProps.teamId);
          body.append('dataJson', JSON.stringify(groupToInstall));
          fetch(jsRoutes.controllers.BehaviorImportExportController.doImport().url, {
            credentials: 'same-origin',
            headers: headers,
            method: 'POST',
            body: body
          }).then((response) => response.json())
            .then((installedGroup) => {
              this.recentlyInstalled = this.recentlyInstalled.concat(installedGroup);
              this.reload({
                recentlyInstalled: this.recentlyInstalled
              });
            })
            .catch(() => {
              // TODO: Handle errors importing
            });
        }

        behaviorGroupAction(url, behaviorGroupIds) {
          fetch(url, {
            credentials: 'same-origin',
            method: 'POST',
            headers: {
              'Accept': 'application/json',
              'Content-Type': 'application/json',
              'Csrf-Token': this.defaultProps.csrfToken
            },
            body: JSON.stringify({
              behaviorGroupIds: behaviorGroupIds
            })
          }).then(() => {
            window.location.reload();
          });
        }

        mergeBehaviorGroups(behaviorGroupIds) {
          var url = jsRoutes.controllers.ApplicationController.mergeBehaviorGroups().url;
          this.behaviorGroupAction(url, behaviorGroupIds);
        }

        deleteBehaviorGroups(behaviorGroupIds) {
          var url = jsRoutes.controllers.ApplicationController.deleteBehaviorGroups().url;
          this.behaviorGroupAction(url, behaviorGroupIds);
        }

        reload(newProps) {
          const behaviorListProps = Object.assign({}, this.defaultProps, this.recentProps, newProps);
          this.recentProps = Object.assign({}, behaviorListProps);

          behaviorListProps.behaviorGroups = behaviorListProps.behaviorGroups.map(BehaviorGroup.fromJson);
          behaviorListProps.publishedBehaviorGroups = behaviorListProps.publishedBehaviorGroups.map(BehaviorGroup.fromJson);
          behaviorListProps.recentlyInstalled = behaviorListProps.recentlyInstalled.map(BehaviorGroup.fromJson);

          ReactDOM.render(
            React.createElement(BehaviorList, behaviorListProps),
            document.getElementById(behaviorListProps.containerId)
          );
        }
      }

      const loader = new BehaviorListLoader(BehaviorListConfig);

      loader.reload();
      loader.loadPublishedBehaviorGroups();
    }
  );
});
