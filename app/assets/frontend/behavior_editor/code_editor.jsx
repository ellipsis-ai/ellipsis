import * as React from 'react';
import Codemirror from '../shared_ui/react-codemirror';
import {JSHINT} from 'jshint';
window.JSHINT = JSHINT;
import 'codemirror';
import 'codemirror/mode/javascript/javascript';
import 'codemirror/addon/lint/lint';
import 'codemirror/addon/lint/javascript-lint';
import 'codemirror/addon/hint/show-hint';
import 'codemirror/addon/edit/closebrackets';
import 'codemirror/addon/fold/foldcode';
import 'codemirror/addon/fold/foldgutter';
import 'codemirror/addon/fold/brace-fold';
import 'codemirror/addon/comment/comment';

const CodeEditor = React.createClass({
  propTypes: {
    firstLineNumber: React.PropTypes.oneOfType([
      React.PropTypes.number,
      React.PropTypes.string
    ]).isRequired,
    functionParams: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
    lineWrapping: React.PropTypes.bool,
    onChange: React.PropTypes.func.isRequired,
    onCursorChange: React.PropTypes.func.isRequired,
    value: React.PropTypes.string.isRequired,
    autocompletions: React.PropTypes.arrayOf(React.PropTypes.string).isRequired
  },
  getJsHintOptions: function() {
    return {
      // Enforcing options
      bitwise: false,
      curly: false,
      eqeqeq: false,
      esversion: 6,
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
      predef: this.props.functionParams,
      shadow: false,
      singleGroups: false,
      strict: false,
      undef: true,
      unused: false,
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
      prototypejs: false,
      qunit: false,
      rhino: false,
      shelljs: false,
      typed: false,
      worker: false,
      wsh: false,
      yui: false
    };
  },

  autocompleteParams: function(cm) {
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
    };
  },

  replaceTabsWithSpaces: function(cm) {
    var spaces = Array(cm.getOption("indentUnit") + 1).join(" ");
    cm.replaceSelection(spaces);
  },

  refresh: function() {
    this.refs.codemirror.refresh();
  },

  render: function() {
    return (
      <Codemirror
        ref="codemirror"
        value={this.props.value}
        onChange={this.props.onChange}
        onCursorChange={this.props.onCursorChange}
        options={{
          mode: "javascript",
          firstLineNumber: this.props.firstLineNumber,
          gutters: ["CodeMirror-lint-markers","CodeMirror-linenumbers", "CodeMirror-foldgutter"],
          hintOptions: { hint: this.autocompleteParams },
          indentUnit: 2,
          indentWithTabs: false,
          lineWrapping: this.props.lineWrapping,
          lineNumbers: true,
          lint: {
            options: this.getJsHintOptions()
          },
          smartIndent: true,
          tabSize: 2,
          viewportMargin: Infinity,
          autoCloseBrackets: true,
          foldGutter: true,
          extraKeys: {
            "Esc": "autocomplete",
            "Tab": this.replaceTabsWithSpaces,
            "Cmd-/": "toggleComment"
          }
        }}
      />
    );
  }
});

export default CodeEditor;
