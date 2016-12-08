/* global BehaviorGroupImporterConfig:false */

requirejs(['../common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './behavior_importer/index', './models/behavior_version'],
    function(Core, Fetch, React, ReactDOM, BehaviorImporter, BehaviorVersion) {
    var props = Object.assign({}, BehaviorGroupImporterConfig);
    props.behaviorGroups.forEach((group) => {
      group.behaviorVersions = group.behaviorVersions.map(BehaviorVersion.fromJson);
    });
    var myBehaviorImporter = React.createElement(BehaviorImporter, props);
    ReactDOM.render(myBehaviorImporter, document.getElementById(props.containerId));
  });
});
