/* global BehaviorEditorConfiguration:false */

requirejs(['../common'], function() {
  requirejs(
    ['core-js', 'react', 'react-dom', './behavior_editor/index', './models/trigger'],
    function(Core, React, ReactDOM, BehaviorEditor, Trigger) {
      var config = Object.assign({}, BehaviorEditorConfiguration);
      var additionalData = {
        csrfToken: config.csrfToken,
        envVariables: config.envVariables,
        paramTypes: config.paramTypes,
        oauth2Applications: config.oauth2Applications,
        oauth2Apis: config.oauth2Apis,
        justSaved: config.justSaved,
        notifications: config.notifications
      };
      config.data.triggers = Trigger.triggersFromJson(config.data.triggers);
      var myBehaviorEditor = React.createElement(BehaviorEditor, Object.assign(config.data, additionalData));
      ReactDOM.render(myBehaviorEditor, document.getElementById(config.containerId));
    }
  );
});
