import React from 'react';
import Codemirror from '../shared_ui/react-codemirror';
import HelpButton from '../help/help_button';
import ResponseTemplate from '../models/response_template';
import SectionHeading from '../shared_ui/section_heading';
import ToggleGroup from '../form/toggle_group';

const ResponseTemplateConfiguration = React.createClass({
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

          <div className="mbl ptxl">
            <SectionHeading number={this.props.sectionNumber}>
              <span className="mrm">Response</span>
              <span className="display-inline-block">
                <HelpButton onClick={this.props.onToggleHelp} toggled={this.props.helpVisible}/>
              </span>
            </SectionHeading>

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

export default ResponseTemplateConfiguration;
