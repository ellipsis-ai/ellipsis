/* global BehaviorImporterConfig:false */

requirejs(['../common'], function() {
  requirejs(['core-js', 'react', 'react-dom', './behavior_importer/index'], function(Core, React, ReactDOM, BehaviorImporter) {
    var config = BehaviorGroupImporterConfig;
    var myBehaviorImporter = React.createElement(BehaviorImporter, config);
    ReactDOM.render(myBehaviorImporter, document.getElementById(config.containerId));
  });
});
