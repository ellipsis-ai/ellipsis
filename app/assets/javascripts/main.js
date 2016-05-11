requirejs.config({
    paths:{
        'react': 'https://cdnjs.cloudflare.com/ajax/libs/react/15.0.2/react',
        'react-dom': 'https://cdnjs.cloudflare.com/ajax/libs/react/15.0.2/react-dom',
        'codemirror': ['codemirror/lib/codemirror']
    }
});

define(function(require) {
  var myBehaviorEditor = require('./behavior_editor');
  myBehaviorEditor.load({
    description: "Bang two coconuts together",
    nodeFunction: 'function(userInput1, onSuccess, onError) {\n' +
      '  onSuccess("*clop clop clop clop*\\nYou have arrived at " + userInput);\n' +
      '}\n',
    args: [{ name: 'userInput1', question: '' }]
  });
});
