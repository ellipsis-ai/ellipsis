requirejs.config({
    paths:{
        'react': 'https://cdnjs.cloudflare.com/ajax/libs/react/15.0.2/react',
        'react-dom': 'https://cdnjs.cloudflare.com/ajax/libs/react/15.0.2/react-dom',
    },
    packages: [{
      name: 'codemirror',
      location: 'codemirror/',
      main: 'lib/codemirror'
    }]
});

define(function(require) {
  var myBehaviorEditor = require('./behavior_editor');
  var data = JSON.parse(document.getElementById('editorData').text);
  myBehaviorEditor.load(data);
});
