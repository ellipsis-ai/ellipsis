/* global BehaviorImporterConfig:false */
requirejs(['../common'], function() {
  requirejs(['react', 'react-dom', './behavior_importer/index'], function(React, ReactDOM, BehaviorImporter) {
    var config = BehaviorImporterConfig;
    var myBehaviorImporter = React.createElement(BehaviorImporter, config);
    ReactDOM.render(myBehaviorImporter, document.getElementById(config.containerId));
  });
});
