/* global BehaviorListConfig:false */

requirejs(['../common'], function() {
  requirejs(
    ['core-js', 'whatwg-fetch', 'react', 'react-dom', './behavior_list/index', './models/behavior_group'],
    function(Core, Fetch, React, ReactDOM, BehaviorList, BehaviorGroup) {

      function loadPublishedBehaviorGroups() {
        fetch(jsRoutes.controllers.ApplicationController.fetchPublishedBehaviorInfo().url, {
          credentials: 'same-origin'
        }).then((response) => response.json())
          .then((json) => {
            reload({
              publishedBehaviorGroups: json.map(BehaviorGroup.fromJson),
              hasLoadedPublishedBehaviorGroups: true
            });
          }).catch(() => {
            // TODO: figure out what to do if there's a request error
            console.log('oops');
          });
      }

      function reload(newProps) {
        const behaviorListProps = Object.assign({}, BehaviorListConfig, {
          onLoadPublishedBehaviorGroups: loadPublishedBehaviorGroups,
          hasLoadedPublishedBehaviorGroups: false,
          publishedBehaviorGroups: []
        }, newProps);
        behaviorListProps.behaviorGroups = behaviorListProps.behaviorGroups.map(BehaviorGroup.fromJson);
        behaviorListProps.publishedBehaviorGroups = behaviorListProps.publishedBehaviorGroups.map(BehaviorGroup.fromJson);

        ReactDOM.render(
          React.createElement(BehaviorList, behaviorListProps),
          document.getElementById(behaviorListProps.containerId)
        );
      }

      reload();

    }
  );
});
