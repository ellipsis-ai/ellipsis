/* global BehaviorEditorConfiguration:false */

requirejs(['../common'], function() {
  requirejs(['react', 'react-dom', './behavior_editor/index'], function(React, ReactDOM, BehaviorEditor) {
    var config = BehaviorEditorConfiguration;
    var additionalData = { csrfToken: config.csrfToken, envVariableNames: config.envVariableNames, justSaved: config.justSaved };
    var myBehaviorEditor = React.createElement(BehaviorEditor, Object.assign(config.data, additionalData));
    ReactDOM.render(myBehaviorEditor, document.getElementById(config.containerId));
  });
});
