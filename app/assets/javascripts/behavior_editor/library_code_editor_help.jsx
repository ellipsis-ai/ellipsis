define(function(require) {
  var React = require('react'),
    Constants = require('../lib/constants'),
    Checklist = require('./checklist');

  return React.createClass({
    displayName: "LibraryCodeEditorHelp",
    propTypes: {
      functionBody: React.PropTypes.string.isRequired,
      isFinished: React.PropTypes.bool.isRequired,
      onToggleHelp: React.PropTypes.func.isRequired,
      helpIsActive: React.PropTypes.bool.isRequired
    },

    hasCalledRequire: function() {
      var code = this.props.functionBody;
      return /\brequire\(\s*\S.+?\)/.test(code);
    },

    hasReturned: function() {
      var code = this.props.functionBody;
      return /return/.test(code);
    },

    hasCode: function() {
      return /\S/.test(this.props.functionBody);
    },

    render: function() {
      return (
        <div>
          <Checklist disabledWhen={this.props.isFinished}>
            <Checklist.Item checkedWhen={this.hasReturned()} hiddenWhen={this.props.isFinished}>
              <span>Write a Node.js (<a href={Constants.NODE_JS_DOCS_URL} target="_blank">{Constants.NODE_JS_VERSION}</a>) </span>
              <span>function that returns an object or function that can be <code>require()</code>’d elsewhere.</span>
            </Checklist.Item>
          </Checklist>
        </div>
      );
    }
  });
});
