/*
RequireJS production configuration
Development configuration lives in common.js
*/

// Note: using pre-minified packages can be harmful!
// React prefers to have the pre-minified version in production, but moment.js and URI.js
// break things when the pre-minified versions are require()'d.

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
      main: 'es6-promise.min'
    }, {
      name: 'whatwg-fetch',
      location: '../lib/fetch',
      main: 'fetch'
    }, {
      name: 'javascript-debounce',
      location: '../lib/javascript-debounce/dist',
      main: 'javascript-debounce.min'
    }, {
      name: 'moment',
      location: '../lib/moment',
      main: 'moment'
    }, {
      name: 'react',
      location: '../lib/react',
      main: 'react.min'
    }, {
      name: 'react-dom',
      location: '../lib/react',
      main: 'react-dom.min'
    }, {
      name: 'urijs',
      location: '../lib/urijs/src',
      main: 'URI'
    }
  ],
  modules: [
    {
      name: 'common',
      include: [
        'es6-promise',
        'javascript-debounce',
        'react',
        'react-dom',
        'urijs'
      ]
    }, {
      name: 'behavior_editor/loader',
      include: ['behavior_editor/index'],
      exclude: ['common']
    }, {
      name: 'behavior_importer/loader',
      include: ['behavior_importer/index'],
      exclude: ['common']
    }, {
      name: 'behavior_list/loader',
      include: ['behavior_list/index'],
      exclude: ['common']
    }
  ]
});
