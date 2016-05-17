requirejs.config({
  paths:{
    'react': 'https://cdnjs.cloudflare.com/ajax/libs/react/15.0.2/react',
    'react-dom': 'https://cdnjs.cloudflare.com/ajax/libs/react/15.0.2/react-dom',
  },
  packages: [{
    name: 'codemirror',
    location: 'codemirror',
    main: 'lib/codemirror'
  }]
});

define(['module'], function(module) {
  require(['behavior_editor'], function(behaviorEditor) {
    behaviorEditor.load(module.config().data);
  });
});
