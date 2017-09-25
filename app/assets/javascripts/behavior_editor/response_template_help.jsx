define(function(require) {
var React = require('react'),
  Checklist = require('./checklist'),
  HelpPanel = require('../help/panel'),
  ResponseTemplate = require('../models/response_template');

return React.createClass({
  propTypes: {
    firstParamName: React.PropTypes.string,
    template: React.PropTypes.instanceOf(ResponseTemplate),
    onCollapseClick: React.PropTypes.func.isRequired
  },

  getUserParamTemplateHelp: function() {
    return (
      <Checklist.Item checkedWhen={this.props.template && this.props.template.includesAnyParam()}>
        Repeat back user input:<br />
        <div className="box-code-example">
          You said {this.getExampleParamName()}
        </div>
      </Checklist.Item>
    );
  },

  getExampleParamName: function() {
    return this.props.firstParamName ? `{${this.props.firstParamName}}` : "{exampleParamName}";
  },

  getSuccessResultTemplateHelp: function() {
    return (
      <Checklist.Item checkedWhen={this.props.template && this.props.template.includesSuccessResult()}>
        Say the result provided to <code>ellipsis.success</code>, if it’s a string:<br />
        <div className="box-code-example">
          The answer is {"{successResult}"}
        </div>
      </Checklist.Item>
    );
  },

  getPathTemplateHelp: function() {
    return (
      <Checklist.Item checkedWhen={this.props.template && this.props.template.includesPath()}>
        Include properties of the result if it’s an object:<br />
        <div className="box-code-example">
          Name: {"{successResult.user.name}"}
        </div>
      </Checklist.Item>
    );
  },

  getIterationTemplateHelp: function() {
    return (
      <Checklist.Item checkedWhen={this.props.template && this.props.template.includesIteration()}>
        Iterate through a list/array of items:<br />
        <pre className="box-code-example">{
`{for item in successResult.items}
* {item}
{endfor}`
        }</pre>
      </Checklist.Item>
    );
  },

  getLogicHelp: function() {
    return (
      <Checklist.Item checkedWhen={this.props.template && this.props.template.includesIfLogic()}>
        Use if/else logic with strict boolean values:<br />
        <pre className="box-code-example">{
`{if successResult.booleanValue}
It worked
{else}
Something went wrong.
{endif}`
        }</pre>
      </Checklist.Item>
    );
  },

  render: function() {
    return (
      <HelpPanel
        heading="Response template"
        onCollapseClick={this.props.onCollapseClick}
      >
        <p>
          <span>Write a response for Ellipsis to send when the action is complete. You can use </span>
          <span><a href="http://commonmark.org/help/" target="_blank">Markdown</a> formatting, if desired.</span>
        </p>

        <p>Use the following methods to insert data and add basic logic:</p>

        <Checklist className="mtxs list-space-l" disabledWhen={false}>
          {this.getUserParamTemplateHelp()}
          {this.getSuccessResultTemplateHelp()}
          {this.getPathTemplateHelp()}
          {this.getIterationTemplateHelp()}
          {this.getLogicHelp()}
        </Checklist>

      </HelpPanel>
    );
  }
});

});
