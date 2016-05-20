/*
RequireJS production configuration
Development configuration lives in main.js
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
      main: 'react.min'
    }, {
      name: 'react-dom',
      location: '../lib/react',
      main: 'react-dom.min'
    }
  ]
})
