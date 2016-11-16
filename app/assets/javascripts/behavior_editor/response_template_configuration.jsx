define(function(require) {
  var React = require('react'),
    Checklist = require('./checklist'),
    Codemirror = require('../react-codemirror'),
    Param = require('../models/param'),
    ResponseTemplate = require('../models/response_template'),
    SectionHeading = require('./section_heading'),
    ToggleGroup = require('../form/toggle_group');

  return React.createClass({
    displayName: 'ResponseTemplateConfiguration',
    propTypes: {
      template: React.PropTypes.instanceOf(ResponseTemplate).isRequired,
      onChangeTemplate: React.PropTypes.func.isRequired,
      isFinishedBehavior: React.PropTypes.bool.isRequired,
      behaviorUsesCode: React.PropTypes.bool.isRequired,
      shouldForcePrivateResponse: React.PropTypes.bool.isRequired,
      onChangeForcePrivateResponse: React.PropTypes.func.isRequired,
      onCursorChange: React.PropTypes.func.isRequired,
      userParams: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Param)).isRequired
    },

    getTemplateDataHelp: function() {
      if (this.props.behaviorUsesCode) {
        return (
          <div>
            <span>You can include data in your response.<br /></span>
            <Checklist className="mtxs" disabledWhen={this.props.isFinishedBehavior}>
              {this.getUserParamTemplateHelp()}
              {this.getSuccessResultTemplateHelp()}
              {this.getPathTemplateHelp()}
              {this.getIterationTemplateHelp()}
            </Checklist>
          </div>
        );
      }
    },

    getUserParamTemplateHelp: function() {
      return (
        <Checklist.Item checkedWhen={this.props.template.includesAnyParam()}>
          User-supplied parameters:<br />
          <div className="box-code-example">
            You said {this.getExampleParamName()}
          </div>
        </Checklist.Item>
      );
    },

    getExampleParamName: function() {
      var firstParamName = this.props.userParams[0] && this.props.userParams[0].name;
      return firstParamName ? `{${firstParamName}}` : "{exampleParamName}";
    },

    getSuccessResultTemplateHelp: function() {
      return (
        <Checklist.Item checkedWhen={this.props.template.includesSuccessResult()}>
          The result provided to <code>ellipsis.success</code>:<br />
          <div className="box-code-example">
            The answer is {"{successResult}"}
          </div>
        </Checklist.Item>
      );
    },

    getPathTemplateHelp: function() {
      return (
        <Checklist.Item checkedWhen={this.props.template.includesPath()}>
          Properties of the result:<br />
          <div className="box-code-example">
            Name: {"{successResult.user.name}"}
          </div>
        </Checklist.Item>
      );
    },

    getIterationTemplateHelp: function() {
      return (
        <Checklist.Item checkedWhen={this.props.template.includesIteration()}>
          Iterating through a list:<br />
          <div className="box-code-example">
            {"{for item in successResult.items}"}<br />
            &nbsp;* {"{item}"}<br />
            {"{endfor}"}
          </div>
        </Checklist.Item>
      );
    },

    unsetForcePrivateResponse: function() {
      this.props.onChangeForcePrivateResponse(false);
    },

    setForcePrivateResponse: function() {
      this.props.onChangeForcePrivateResponse(true);
    },

    render: function() {
      return (
        <div className="columns">

          <div className="column column-one-quarter mobile-column-full mbxl mobile-mbs type-s">

            <SectionHeading>Then Ellipsis will respond with</SectionHeading>

            <Checklist disabledWhen={this.props.isFinishedBehavior}>
              <Checklist.Item checkedWhen={this.props.template.usesMarkdown()}>
                <span>Use <a href="http://commonmark.org/help/" target="_blank">Markdown</a> </span>
                <span>to format the response, add links, etc.</span>
              </Checklist.Item>
              {this.props.behaviorUsesCode ? "" : (
                <Checklist.Item>Add code above if you want to collect user input before returning a response.</Checklist.Item>
              )}
            </Checklist>

            {this.getTemplateDataHelp()}
          </div>

          <div className="column column-three-quarters mobile-column-full pll mobile-pln mbxxxl">
            <div className="border-top border-left border-right pam">
              <ToggleGroup className="form-toggle-group-s align-m">
                <ToggleGroup.Item
                  title="Ellipsis will respond wherever you talk to it"
                  label="Respond normally"
                  activeWhen={!this.props.shouldForcePrivateResponse}
                  onClick={this.unsetForcePrivateResponse}
                />
                <ToggleGroup.Item
                  title="Ellipsis will always respond in a private message"
                  label="Respond privately"
                  activeWhen={this.props.shouldForcePrivateResponse}
                  onClick={this.setForcePrivateResponse}
                />
              </ToggleGroup>
            </div>
            <div className="position-relative CodeMirror-container-no-gutter pbm">
              <Codemirror value={this.props.template.toString()}
                onChange={this.props.onChangeTemplate}
                onCursorChange={this.props.onCursorChange}
                options={{
                  mode: {
                    name: "markdown",
                    /* Use CommonMark-appropriate settings */
                    fencedCodeBlocks: true,
                    underscoresBreakWords: false
                  },
                  gutters: ['CodeMirror-no-gutter'],
                  indentUnit: 4,
                  indentWithTabs: true,
                  lineWrapping: true,
                  lineNumbers: false,
                  smartIndent: true,
                  tabSize: 4,
                  viewportMargin: Infinity,
                  placeholder: "The result is {successResult}"
                }}
              />
            </div>
          </div>
        </div>

      );
    }
  });
});
