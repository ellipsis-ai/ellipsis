/*
RequireJS development configuration
Production configuration lives in build.js.
*/

requirejs.config({
  packages: [
    {
      name: 'codemirror',
      location: 'codemirror',
      main: 'lib/codemirror'
    }, {
      name: 'react',
      location: '../lib/react',
      main: 'react'
    }, {
      name: 'react-dom',
      location: '../lib/react',
      main: 'react-dom'
    }
  ]
});

requirejs(['./behavior_editor'], function(behaviorEditor) {
  behaviorEditor.load(BehaviorEditorConfiguration.data, BehaviorEditorConfiguration.containerId, BehaviorEditorConfiguration.csrfToken);
});
