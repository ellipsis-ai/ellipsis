/*
RequireJS production configuration
Development configuration lives in common.js
*/

// Note: using pre-minified packages can be harmful!
// React prefers to have the pre-minified version in production, but moment.js and URI.js
// break things when the pre-minified versions are require()'d.

requirejs.config({
  baseUrl: '/assets/javascripts/',

  // Helps trim some unwanted assets
  fileExclusionRegExp: /(^\.|\btests?\b|\btheme\b|jshint-rhino|jshint\.ai)/,

  // Stops r.js from unnecessarily minifying non-build assets
  skipDirOptimize: true,

  // Exclude dynamic data paths from optimization using special empty: syntax
  paths: {
    'config': 'empty:'
  },

  packages: [
    {
      name: 'codemirror',
      location: '../lib/codemirror',
      main: 'lib/codemirror'
    }, {
      name: 'core-js',
      location: '../lib/core.js/client',
      main: 'shim.min'
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
      name: 'node-uuid',
      location: '../lib/node-uuid',
      main: 'uuid'
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
        'core-js',
        'javascript-debounce',
        'node-uuid',
        'react',
        'react-dom',
        'urijs',
        'whatwg-fetch'
      ]
    }, {
      name: 'behavior_editor/loader',
      include: ['behavior_editor/index'],
      exclude: ['common']
    }, {
      name: 'behavior_list/loader',
      include: ['behavior_list/app'],
      exclude: ['common']
    }, {
      name: 'application_editor/loader',
      include: ['application_editor/index'],
      exclude: ['common']
    }, {
      name: 'settings/integrations/loader',
      include: ['integrations/index'],
      exclude: ['common']
    }, {
      name: 'settings/aws_config_editor/loader',
      include: ['aws_config_editor/index'],
      exclude: ['common']
    }, {
      name: 'settings/aws_config_list/loader',
      include: ['aws_config_list/index'],
      exclude: ['common']
    }, {
      name: 'api_token_generator/loader',
      include: ['api_token_generator/index'],
      exclude: ['common']
    }, {
      name: 'settings/environment_variables/loader',
      include: ['environment_variables/index'],
      exclude: ['common']
    }, {
      name: 'settings/regional_settings/loader',
      include: ['regional_settings/index'],
      exclude: ['common']
    }, {
      name: 'styleguide/colors/loader',
      include: ['styleguide/colors/index'],
      exclude: ['common']
    }, {
      name: 'scheduling/loader',
      include: ['scheduling/index'],
      exclude: ['common']
    }, {
      name: 'github_config/loader',
      include: ['github_config/index'],
      exclude: ['common']
    }
  ]
});
