requirejs.config({
    paths:{
        'react': 'https://cdnjs.cloudflare.com/ajax/libs/react/15.0.2/react',
        'react-dom': 'https://cdnjs.cloudflare.com/ajax/libs/react/15.0.2/react-dom',
        'react-codemirror': ['../../assets/javascripts/react-codemirror']
    },
    shim: {
        'react-codemirror': {
            deps: ['react','classnames','lodash.debounce']
        }
    }
});

define(function(require) {
  var myBehaviorEditor = require('./behavior_editor');
  myBehaviorEditor({
    description: "Bang two coconuts together",
    nodeFunction: 'function(userInput, onSuccess, onError) {\n' +
      '  onSuccess("*clop clop clop clop*\\nYou have arrived at " + userInput);\n' +
      '}\n',
    argNames: ['userInput']
  });
});
