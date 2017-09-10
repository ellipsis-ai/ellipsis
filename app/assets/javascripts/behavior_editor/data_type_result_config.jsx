define(function(require) {
  var React = require('react'),
    SectionHeading = require('../shared_ui/section_heading'),
    HelpButton = require('../help/help_button'),
    ToggleGroup = require('../form/toggle_group');

  return React.createClass({
    displayName: 'DataTypeResultConfig',
    propTypes: {
      usesSearch: React.PropTypes.bool.isRequired,
      onChange: React.PropTypes.func.isRequired,
      isFinishedBehavior: React.PropTypes.bool.isRequired,
      onToggleActivePanel: React.PropTypes.func.isRequired,
      activePanelName: React.PropTypes.string
    },

    onUseSearch: function() {
      this.props.onChange(true);
    },

    onUseList: function() {
      this.props.onChange(false);
    },

    togglePromptHelp: function() {
      this.props.onToggleActivePanel('helpForDataTypePrompt');
    },

    render: function() {
      return (
        <div className="container ptxl pbxxl">
          <SectionHeading number="2">
            <span className="mrm">How to prompt the user</span>
            <span className="display-inline-block">
              <HelpButton onClick={this.togglePromptHelp} toggled={this.props.activePanelName === 'helpForDataTypePrompt'} />
            </span>
          </SectionHeading>

          <div className="mtxl">
            <ToggleGroup className="align-m">
              <ToggleGroup.Item
                title="Your function should return a list of choices. The user will be asked to select one."
                label="Run the function first"
                activeWhen={!this.props.usesSearch}
                onClick={this.onUseList}
              />
              <ToggleGroup.Item
                title="Your function will receive the input, and can use it to parse, filter or search before returning a list."
                label="Ask for user input first"
                activeWhen={this.props.usesSearch}
                onClick={this.onUseSearch}
              />
            </ToggleGroup>
          </div>
        </div>
      );
    }
  });
});
