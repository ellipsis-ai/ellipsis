requirejs.config({
  paths:{
    'react': ['../lib/react'],
    'react-dom': ['../lib/react-dom/dist'],
  },
  packages: [
    {
      name: 'codemirror',
      location: 'codemirror',
      main: 'lib/codemirror'
    }, {
      name: 'react',
      main: 'react'
    }, {
      name: 'react-dom',
      main: 'react-dom'
    }
  ],
  shim: {
    'react': {
      exports: 'react'
    },
    'react-dom': {
      deps: ['react'],
      exports: 'react-dom'
    }
  }
});

define(['module', './behavior_editor'], function(module, behaviorEditor) {
  behaviorEditor.load(module.config().data, module.config().containerId);
});
