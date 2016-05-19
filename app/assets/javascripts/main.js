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

define(['module', './behavior_editor'], function(module, behaviorEditor) {
  behaviorEditor.load(module.config().data, module.config().containerId);
});
