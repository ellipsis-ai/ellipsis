define(function(require) {
var React = require('react'),
  Codemirror = require('./react-codemirror'),
  CodemirrorJSMode = require('./codemirror/mode/javascript/javascript'),
  CodeMirrorLint = require('./codemirror/addon/lint/lint'),
  CodeMirrorJSLint = require('./codemirror/addon/lint/javascript-lint'),
  CodemirrorShowHint = require('./codemirror/addon/hint/show-hint');

return React.createClass({
  getJsHintOptions: function() {
    return {
      // Enforcing options
      bitwise: false,
      curly: false,
      eqeqeq: false,
      esversion: 5,
      forin: false,
      freeze: false,
      funcscope: false,
      globals: true,
      iterator: false,
      latedef: false,
      noarg: false,
      nocomma: false,
      nonbsp: false,
      nonew: false,
      notypeof: true,
      predef: ['onSuccess', 'onError', 'ellipsis'],
      shadow: false,
      singleGroups: false,
      strict: false,
      undef: true,
      unused: true,
      varstmt: false,

      // Relaxing options
      asi: true,
      boss: true,
      eqnull: true,
      evil: true,
      expr: true,
      lastsemic: true,
      loopfunc: true,
      noyield: true,
      plusplus: false,
      proto: true,
      scripturl: true,
      supernew: true,
      withstmt: true,

      // Environment
      browser: false,
      browserify: false,
      couch: false,
      devel: false,
      dojo: false,
      jasmine: false,
      jquery: false,
      mocha: false,
      module: false,
      mootools: false,
      node: true,
      nonstandard: true,
      phantom: false,
      prototype: false,
      qunit: false,
      rhino: false,
      shelljs: false,
      typed: false,
      worker: false,
      wsh: false,
      yui: false
    };
  },

  autocompleteParams: function(cm, options) {
    var matches = [];
    var possibleWords = this.props.autocompletions;

    var cursor = cm.getCursor();
    var line = cm.getLine(cursor.line);
    var start = cursor.ch;
    var end = cursor.ch;

    while (start && /\w/.test(line.charAt(start - 1))) {
      --start;
    }
    while (end < line.length && /\w/.test(line.charAt(end))) {
      ++end;
    }

    var word = line.slice(start, end).toLowerCase();

    possibleWords.forEach(function(w) {
      var lowercase = w.toLowerCase();
      if (lowercase.indexOf(word) !== -1) {
        matches.push(w);
      }
    });

    return {
      list: matches,
      from: { line: cursor.line, ch: start },
      to: { line: cursor.line, ch: end }
    }
  },

  render: function() {
    return (
      <Codemirror value={this.props.value}
        onChange={this.props.onChange}
        options={{
          mode: "javascript",
          firstLineNumber: this.props.firstLineNumber,
          gutters: ["CodeMirror-lint-markers"],
          hintOptions: { hint: this.autocompleteParams },
          indentUnit: 2,
          indentWithTabs: false,
          lineWrapping: this.props.lineWrapping,
          lineNumbers: true,
          lint: this.getJsHintOptions(),
          smartIndent: true,
          tabSize: 2,
          viewportMargin: Infinity,
          extraKeys: {
            Esc: "autocomplete",
            Tab: function(cm) {
              var spaces = Array(cm.getOption("indentUnit") + 1).join(" ");
              cm.replaceSelection(spaces);
            }
          }
        }}
      />
    );
  }
});

});