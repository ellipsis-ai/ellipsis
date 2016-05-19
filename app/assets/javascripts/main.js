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
      location: '../lib/react-dom/dist',
      main: 'react-dom'
    }
  ]
});

define(['./behavior_editor'], function(behaviorEditor) {
  behaviorEditor.load(BehaviorEditorConfiguration.data, BehaviorEditorConfiguration.containerId);
});
