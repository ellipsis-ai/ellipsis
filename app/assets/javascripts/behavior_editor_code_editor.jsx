define(function(require) {
var React = require('react'),
  Codemirror = require('./react-codemirror'),
  CodemirrorJSMode = require('./codemirror/mode/javascript/javascript'),
  CodemirrorShowHint = require('./codemirror/addon/hint/show-hint');

return React.createClass({
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
          hintOptions: { hint: this.autocompleteParams },
          indentUnit: 2,
          indentWithTabs: false,
          lineWrapping: this.props.lineWrapping,
          lineNumbers: true,
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