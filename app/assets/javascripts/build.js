/*
RequireJS production configuration
Development configuration lives in main.js
*/

requirejs.config({
  packages: [
    {
      name: 'classnames',
      location: '../lib/classnames',
      main: 'classnames'
    }, {
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
      main: 'react.min'
    }, {
      name: 'react-codemirror',
      location: '../lib/react-codemirror',
      main: 'react-codemirror'
    }, {
      name: 'react-dom',
      location: '../lib/react',
      main: 'react-dom.min'
    }
  ]
});
