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
      name: 'es6-promise',
      location: '../lib/es6-promise',
      main: 'es6-promise'
    }, {
      name: 'fetch',
      location: '../lib/fetch',
      main: 'fetch'
    }, {
      name: 'javascript-debounce',
      location: '../lib/javascript-debounce/dist',
      main: 'javascript-debounce'
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

requirejs(['react', 'react-dom', './behavior_editor'], function(React, ReactDOM, BehaviorEditor) {
  var config = BehaviorEditorConfiguration;
  var additionalData = { csrfToken: config.csrfToken, envVariableNames: config.envVariableNames, justSaved: config.justSaved };
  var myBehaviorEditor = React.createElement(BehaviorEditor, Object.assign(config.data, additionalData));
  ReactDOM.render(myBehaviorEditor, document.getElementById(config.containerId));
});
