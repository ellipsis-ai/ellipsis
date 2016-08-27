/* global BehaviorEditorConfiguration:false */

requirejs(['../common'], function() {
  requirejs(['react', 'react-dom', './behavior_editor/index'], function(React, ReactDOM, BehaviorEditor) {
    var config = BehaviorEditorConfiguration;
    var additionalData = {
      csrfToken: config.csrfToken,
      envVariables: config.envVariables,
      oauth2Applications: config.oauth2Applications,
      oauth2Apis: config.oauth2Apis,
      justSaved: config.justSaved,
      notifications: config.notifications
    };
    var myBehaviorEditor = React.createElement(BehaviorEditor, Object.assign(config.data, additionalData));
    ReactDOM.render(myBehaviorEditor, document.getElementById(config.containerId));
  });
});
