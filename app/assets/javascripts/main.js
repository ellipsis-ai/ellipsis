/*
RequireJS development configuration
Production configuration lives in build.js.
*/

requirejs.config({
  packages: [
    {
      name: 'codemirror',
      location: '../lib/codemirror',
      main: 'lib/codemirror'
    }, {
      name: 'lodash.debounce',
      location: '../lib/lodash.debounce',
      main: 'lodash.debounce'
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
  behaviorEditor.load(BehaviorEditorConfiguration);
});
