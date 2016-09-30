/* global BehaviorEditorConfiguration:false */

requirejs(['../common'], function() {
  requirejs(
    ['core-js', 'react', 'react-dom', './behavior_editor/index', './models/behavior_version'],
    function(Core, React, ReactDOM, BehaviorEditor, BehaviorVersion) {
      var config = Object.assign({}, BehaviorEditorConfiguration);
      var additionalData = {
        csrfToken: config.csrfToken,
        envVariables: config.envVariables,
        paramTypes: config.paramTypes,
        oauth2Applications: config.oauth2Applications,
        oauth2Apis: config.oauth2Apis,
        justSaved: config.justSaved,
        notifications: config.notifications,
        dataType: config.dataType
      };
      var behaviorEditorProps = BehaviorVersion.fromJson(Object.assign({}, config.data, additionalData));
      var myBehaviorEditor = React.createElement(BehaviorEditor, behaviorEditorProps);
      ReactDOM.render(myBehaviorEditor, document.getElementById(config.containerId));
    }
  );
});
