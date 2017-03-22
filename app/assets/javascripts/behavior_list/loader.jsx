/* global BehaviorListConfig:false */

requirejs(['../common'], function() {
  requirejs(
    ['core-js', './lib/data_request', 'react', 'react-dom', './behavior_list/index', './models/behavior_group'],
    function(Core, DataRequest, React, ReactDOM, BehaviorList, BehaviorGroup) {

      class BehaviorListLoader {
        constructor(defaultProps) {
          this.defaultProps = Object.assign({
            onLoadPublishedBehaviorGroups: this.loadPublishedBehaviorGroups.bind(this),
            onBehaviorGroupImport: this.importBehaviorGroup.bind(this),
            onMergeBehaviorGroups: this.mergeBehaviorGroups.bind(this),
            onDeleteBehaviorGroups: this.deleteBehaviorGroups.bind(this),
            publishedBehaviorGroupLoadStatus: 'loading',
            publishedBehaviorGroups: [],
            recentlyInstalled: []
          }, defaultProps);
          this.recentProps = {};
          this.recentlyInstalled = [];
        }

        loadPublishedBehaviorGroups() {
          const url = jsRoutes.controllers.ApplicationController.fetchPublishedBehaviorInfo(
            this.defaultProps.teamId, this.defaultProps.branchName
          ).url;
          DataRequest.jsonGet(url)
            .then((json) => {
              this.reload({
                publishedBehaviorGroups: json,
                publishedBehaviorGroupLoadStatus: 'loaded'
              });
            })
            .catch(() => {
              this.reload({
                publishedBehaviorGroupLoadStatus: 'error'
              });
            });
        }

        importBehaviorGroup(groupToInstall) {
          const url = jsRoutes.controllers.BehaviorImportExportController.doImport().url;

          const body = {
            teamId: this.recentProps.teamId,
            dataJson: JSON.stringify(groupToInstall)
          };

          DataRequest
            .jsonPost(url, body, this.defaultProps.csrfToken)
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

        mergeBehaviorGroups(behaviorGroupIds) {
          const url = jsRoutes.controllers.ApplicationController.mergeBehaviorGroups().url;
          const body = { behaviorGroupIds: behaviorGroupIds };
          DataRequest
            .jsonPost(url, body, this.defaultProps.csrfToken)
            .then(() => { window.location.reload(); })
            .catch(() => {
              // TODO: Error handling
            });
        }

        deleteBehaviorGroups(behaviorGroupIds) {
          const url = jsRoutes.controllers.ApplicationController.deleteBehaviorGroups().url;
          const body = { behaviorGroupIds: behaviorGroupIds };
          DataRequest
            .jsonPost(url, body, this.defaultProps.csrfToken)
            .then(() => { window.location.reload(); })
            .catch(() => {
              // TODO: Error handling
            });
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
