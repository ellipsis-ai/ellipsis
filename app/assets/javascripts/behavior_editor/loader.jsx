/* global BehaviorEditorConfiguration:false */

requirejs(['../common'], function() {
  requirejs(
    ['core-js', 'react', 'react-dom', './behavior_editor/index', './models/behavior_version'],
    function(Core, React, ReactDOM, BehaviorEditor, BehaviorVersion) {
      var config = Object.assign({}, BehaviorEditorConfiguration);
      var additionalData = {
        csrfToken: config.csrfToken,
        envVariables: config.envVariables,
        otherBehaviorsInGroup: config.otherBehaviorsInGroup,
        paramTypes: config.paramTypes,
        oauth2Applications: config.oauth2Applications,
        oauth2Apis: config.oauth2Apis,
        simpleTokenApis: config.simpleTokenApis,
        linkedOAuth2ApplicationIds: config.linkedOAuth2ApplicationIds,
        justSaved: config.justSaved,
        notifications: config.notifications,
        onSave: reload
      };

      function reload(newData, justSaved) {
        var combinedData = Object.assign({}, newData, additionalData);
        if (justSaved) {
          combinedData.justSaved = true;
        }
        var behaviorEditorProps = BehaviorVersion.fromJson(combinedData);
        var myBehaviorEditor = React.createElement(BehaviorEditor, behaviorEditorProps);
        ReactDOM.render(myBehaviorEditor, document.getElementById(config.containerId));
      }

      reload(config.data, false);
    }
  );
});
