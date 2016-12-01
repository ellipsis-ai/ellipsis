/* global BehaviorGroupImporterConfig:false */

requirejs(['../common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './behavior_importer/index'],
    function(Core, Fetch, React, ReactDOM, BehaviorImporter) {
    var config = BehaviorGroupImporterConfig;
    var myBehaviorImporter = React.createElement(BehaviorImporter, config);
    ReactDOM.render(myBehaviorImporter, document.getElementById(config.containerId));
  });
});
