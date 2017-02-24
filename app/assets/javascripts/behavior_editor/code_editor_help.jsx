define(function(require) {
  var React = require('react'),
    Checklist = require('./checklist');

  return React.createClass({
    propTypes: {
      functionBody: React.PropTypes.string.isRequired,
      isFinishedBehavior: React.PropTypes.bool.isRequired,
      onToggleHelp: React.PropTypes.func.isRequired,
      helpIsActive: React.PropTypes.bool.isRequired,
      hasUserParameters: React.PropTypes.bool.isRequired
    },

    hasCalledNoResponse: function() {
      return /\bellipsis\.noResponse\(\)/.test(this.props.functionBody);
    },

    hasCalledOnError: function() {
      var code = this.props.functionBody;
      return /\bonError\(\s*\S.+?\)/.test(code) ||
        /\bellipsis\.error\(\s*\S.+?\)/.test(code);
    },

    hasCalledOnSuccess: function() {
      var code = this.props.functionBody;
      return /\bonSuccess\([\s\S]*?\)/.test(code) ||
        /\bellipsis\.success\([\s\S]*?\)/.test(code);
    },

    hasCalledRequire: function() {
      var code = this.props.functionBody;
      return /\brequire\(\s*\S.+?\)/.test(code);
    },

    hasCode: function() {
      return /\S/.test(this.props.functionBody);
    },

    render: function() {
      return (
        <div>
          <Checklist disabledWhen={this.props.isFinishedBehavior}>
            <Checklist.Item checkedWhen={this.hasCode()} hiddenWhen={this.props.isFinishedBehavior}>
              <span>Write a Node.js (<a href="https://nodejs.org/docs/latest-v4.x/api/" target="_blank">v4.3.2</a>) </span>
              <span>function. You can <code>require()</code> any </span>
              <span><a href="https://www.npmjs.com/" target="_blank">NPM package</a>.</span>
            </Checklist.Item>

            <Checklist.Item checkedWhen={this.hasCalledOnSuccess() || this.hasCalledOnError() || this.hasCalledNoResponse()}>
              <span>Finish by calling <code>ellipsis.success(…)</code>, <code>ellipsis.error(…)</code>, and/or <code>ellipsis.noResponse()</code>.</span>
            </Checklist.Item>

            <Checklist.Item checkedWhen={this.props.hasUserParameters} hiddenWhen={this.props.isFinishedBehavior && this.props.hasUserParameters}>
              <span>Add inputs above to collect data from the user. The function will receive each one as a parameter.</span>
            </Checklist.Item>

            <Checklist.Item hiddenWhen={!this.props.isFinishedBehavior || this.hasCalledRequire()}>
              <span>Use <code>require(…)</code> to load any NPM package.</span>
            </Checklist.Item>

          </Checklist>
        </div>
      );
    }
  });
});
