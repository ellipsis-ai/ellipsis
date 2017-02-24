define(function(require) {
  var React = require('react'),
    Checklist = require('./checklist'),
    Codemirror = require('../shared_ui/react-codemirror'),
    HelpButton = require('../help/help_button'),
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
      onToggleHelp: React.PropTypes.func.isRequired,
      helpVisible: React.PropTypes.bool.isRequired,
      sectionNumber: React.PropTypes.string.isRequired
    },

    unsetForcePrivateResponse: function() {
      this.props.onChangeForcePrivateResponse(false);
    },

    setForcePrivateResponse: function() {
      this.props.onChangeForcePrivateResponse(true);
    },

    render: function() {
      return (
        <div className="columns container container-wide">

          <div className="mbxxxl ptxl">
            <SectionHeading number={this.props.sectionNumber}>
              <span className="mrm">Then respond</span>
              <span className="display-inline-block">
                <HelpButton onClick={this.props.onToggleHelp} toggled={this.props.helpVisible}/>
              </span>
            </SectionHeading>

            <div className="type-s">
              <Checklist disabledWhen={this.props.isFinishedBehavior}>
                <Checklist.Item checkedWhen={this.props.template.usesMarkdown()}>
                  <span>Use <a href="http://commonmark.org/help/" target="_blank">Markdown</a> </span>
                  <span>to format the response, add links, etc.</span>
                </Checklist.Item>
                {this.props.behaviorUsesCode ? (
                    <Checklist.Item checkedWhen={this.props.template.includesData()}>
                      <span>You can include data in your response. </span>
                      <button type="button" className="button-raw" onClick={this.props.onToggleHelp}>Examples</button>
                    </Checklist.Item>
                  ) : (
                    <Checklist.Item>Add code above if you want to collect user input before returning a response.</Checklist.Item>
                  )}
              </Checklist>
            </div>

            <div className="border-top border-left border-right border-light pas">
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
                  mode: "gfm",
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
