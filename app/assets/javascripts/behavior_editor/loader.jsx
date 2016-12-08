/* global BehaviorEditorConfiguration:false */

requirejs(['../common'], function() {
  requirejs(
    ['core-js', 'whatwg-fetch', 'react', 'react-dom', './behavior_editor/index', './models/behavior_version'],
    function(Core, Fetch, React, ReactDOM, BehaviorEditor, BehaviorVersion) {
      var config = Object.assign({}, BehaviorEditorConfiguration, {
        otherBehaviorsInGroup: BehaviorEditorConfiguration.otherBehaviorsInGroup.map((ea) => BehaviorVersion.fromJson(ea)),
        onSave: reload
      });

      function reload(newData, justSaved) {
        var props = Object.assign({}, config, newData, {
          behavior: BehaviorVersion.fromJson(newData),
          justSaved: !!justSaved
        });
        var myBehaviorEditor = React.createElement(BehaviorEditor, props);
        ReactDOM.render(myBehaviorEditor, document.getElementById(config.containerId));
      }

      reload(config.data, false);
    }
  );
});
