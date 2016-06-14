/*
RequireJS production configuration
Development configuration lives in main.js
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
      main: 'es6-promise.min'
    }, {
      name: 'fetch',
      location: '../lib/fetch',
      main: 'fetch'
    }, {
      name: 'lodash.debounce',
      location: '../lib/lodash.debounce',
      main: 'lodash.debounce'
    }, {
      name: 'react',
      location: '../lib/react',
      main: 'react.min'
    }, {
      name: 'react-dom',
      location: '../lib/react',
      main: 'react-dom.min'
    }
  ]
});
