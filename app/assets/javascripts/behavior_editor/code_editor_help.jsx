define(function(require) {
  var React = require('react'),
    Checklist = require('./checklist'),
    HelpButton = require('../help/help_button'),
    SectionHeading = require('./section_heading');

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
          <SectionHeading>Ellipsis will run code</SectionHeading>

          <Checklist disabledWhen={this.props.isFinishedBehavior}>
            <Checklist.Item checkedWhen={this.hasCode()} hiddenWhen={this.props.isFinishedBehavior}>
              <span>Write a node.js function. You can <code>require()</code> any </span>
              <span><a href="https://www.npmjs.com/" target="_blank">NPM package</a>.</span>
            </Checklist.Item>

            <Checklist.Item checkedWhen={this.hasCalledOnSuccess()} hiddenWhen={this.props.isFinishedBehavior}>
              <span>End the function by calling </span>
              <code className="type-bold">ellipsis.success(<span className="type-regular">…</span>)</code>
              <span> with text or data to include in the response. </span>
              <button type="button" className="button-raw link button-s" onClick={this.props.onToggleHelp}>Examples</button>
            </Checklist.Item>

            <Checklist.Item checkedWhen={this.hasCalledOnError()} hiddenWhen={this.props.isFinishedBehavior}>
              <span>To end with an error message, call </span>
              <code className="type-bold">ellipsis.error(<span className="type-regular">…</span>)</code>
              <span> with a string. </span>
              <button type="button" className="button-raw link button-s" onClick={this.props.onToggleHelp}>Example</button>
            </Checklist.Item>

            <Checklist.Item checkedWhen={this.hasCalledNoResponse()} hiddenWhen={this.props.isFinishedBehavior}>
              <span>To end with no response, call <code className="type-bold">ellipsis.noResponse()</code>.</span>
            </Checklist.Item>

            <Checklist.Item hiddenWhen={!this.props.isFinishedBehavior}>
              <span>Call <code>ellipsis.success(…)</code>, <code>ellipsis.error(…)</code> and/or <code>ellipsis.noResponse()</code> </span>
              <span>to end your function. </span>
              <span className="pls">
                <HelpButton onClick={this.props.onToggleHelp} toggled={this.props.helpIsActive} />
              </span>
            </Checklist.Item>

            <Checklist.Item checkedWhen={this.props.hasUserParameters} hiddenWhen={this.props.isFinishedBehavior && this.props.hasUserParameters}>
              <span>If you need more information from the user, add one or more inputs above </span>
              <span>and the function will receive them as parameters.</span>
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
