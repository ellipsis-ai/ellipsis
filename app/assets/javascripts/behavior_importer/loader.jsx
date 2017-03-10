/* global BehaviorGroupImporterConfig:false */

requirejs(['../common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './behavior_importer/index', './models/behavior_group'],
    function(Core, Fetch, React, ReactDOM, BehaviorImporter, BehaviorGroup) {
    var props = Object.assign({}, BehaviorGroupImporterConfig, {
      behaviorGroups: BehaviorGroupImporterConfig.behaviorGroups.map((ea) => BehaviorGroup.fromJson(ea))
    });
    var myBehaviorImporter = React.createElement(BehaviorImporter, props);
    ReactDOM.render(myBehaviorImporter, document.getElementById(props.containerId));
  });
});
