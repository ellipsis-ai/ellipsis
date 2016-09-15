/* global BehaviorEditorConfiguration:false */

requirejs(['../common'], function() {
  requirejs(['core-js', 'react', 'react-dom', './behavior_editor/index'], function(Core, React, ReactDOM, BehaviorEditor) {
    var config = BehaviorEditorConfiguration;
    var additionalData = {
      csrfToken: config.csrfToken,
      envVariables: config.envVariables,
      paramTypes: config.paramTypes,
      oauth2Applications: config.oauth2Applications,
      oauth2Apis: config.oauth2Apis,
      justSaved: config.justSaved,
      notifications: config.notifications
    };
    var myBehaviorEditor = React.createElement(BehaviorEditor, Object.assign(config.data, additionalData));
    ReactDOM.render(myBehaviorEditor, document.getElementById(config.containerId));
  });
});
