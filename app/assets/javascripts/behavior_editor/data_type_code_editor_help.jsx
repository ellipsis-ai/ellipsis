define(function(require) {
  var React = require('react'),
    Constants = require('../lib/constants'),
    Checklist = require('./checklist');

  return React.createClass({
    propTypes: {
      functionBody: React.PropTypes.string.isRequired,
      usesSearch: React.PropTypes.bool.isRequired,
      isFinishedBehavior: React.PropTypes.bool.isRequired
    },

    hasCalledOnSuccess: function() {
      var code = this.props.functionBody;
      return /\bonSuccess\([\s\S]*?\)/.test(code) ||
        /\bellipsis\.success\([\s\S]*?\)/.test(code);
    },

    codeUsesSearchQuery: function() {
      var code = this.props.functionBody;
      return /searchQuery/.test(code);
    },

    render: function() {
      return (
        <div>
          <Checklist disabledWhen={this.props.isFinishedBehavior}>
            <Checklist.Item hiddenWhen={this.hasCalledOnSuccess() || this.props.isFinishedBehavior}>
              <span>Write a Node.js (<a href={Constants.NODE_JS_DOCS_URL} target="_blank">{Constants.NODE_JS_VERSION}</a>) </span>
              <span>function that calls <code className="type-bold">ellipsis.success()</code> with an array of items.</span>
            </Checklist.Item>

            <Checklist.Item hiddenWhen={this.props.isFinishedBehavior}>
              <span>Each item should have an <code className="type-bold">id</code> and <code className="type-bold">label</code> property.</span>
            </Checklist.Item>

            <Checklist.Item checkedWhen={this.codeUsesSearchQuery()} hiddenWhen={!this.props.usesSearch || this.props.isFinishedBehavior}>
              <span>You can use the <code className="type-bold">searchQuery</code> parameter to help filter the results</span>
            </Checklist.Item>

          </Checklist>
        </div>
      );
    }
  });
});
