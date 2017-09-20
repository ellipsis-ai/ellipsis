define(function(require) {
  var React = require('react'),
    Constants = require('../lib/constants'),
    Checklist = require('./checklist');

  return React.createClass({
    displayName: "CodeEditorHelp",
    propTypes: {
      functionBody: React.PropTypes.string.isRequired,
      isFinishedBehavior: React.PropTypes.bool.isRequired
    },

    hasCalledOnSuccess: function() {
      var code = this.props.functionBody;
      return /\bellipsis\.success\(.*?\)/.test(code);
    },

    render: function() {
      return (
        <div>
          <Checklist disabledWhen={this.props.isFinishedBehavior}>
            <Checklist.Item checkedWhen={this.hasCalledOnSuccess()} hiddenWhen={this.props.isFinishedBehavior}>
              <span>Write a Node.js (<a href={Constants.NODE_JS_DOCS_URL} target="_blank">{Constants.NODE_JS_VERSION}</a>) </span>
              <span>function that calls <code>ellipsis.success()</code> with a result.</span>
            </Checklist.Item>
          </Checklist>
        </div>
      );
    }
  });
});
