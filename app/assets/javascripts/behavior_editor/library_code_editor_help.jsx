define(function(require) {
  var React = require('react'),
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
            <Checklist.Item checkedWhen={this.hasCode()} hiddenWhen={this.props.isFinished}>
              <span>Write a Node.js (<a href="https://nodejs.org/docs/latest-v6.x/api/" target="_blank">v6.10.2</a>) </span>
              <span>function. You can <code>require()</code> any </span>
              <span><a href="https://www.npmjs.com/" target="_blank">NPM package</a>.</span>
            </Checklist.Item>

            <Checklist.Item checkedWhen={this.hasReturned()}>
              <span>Finish by returning an object or a function that can be <code>require()</code>'d elsewhere in this skill</span>
            </Checklist.Item>

            <Checklist.Item hiddenWhen={!this.props.isFinished || this.hasCalledRequire()}>
              <span>Use <code>require(…)</code> to load any NPM package or local library.</span>
            </Checklist.Item>

          </Checklist>
        </div>
      );
    }
  });
});
