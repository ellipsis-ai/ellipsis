/*
RequireJS development configuration
Production configuration lives in build.js.
*/

requirejs.config({
  baseUrl: '/assets/javascripts/',
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
