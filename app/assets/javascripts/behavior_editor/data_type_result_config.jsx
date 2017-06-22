define(function(require) {
  var React = require('react'),
    SectionHeading = require('../shared_ui/section_heading'),
    Checklist = require('./checklist'),
    ToggleGroup = require('../form/toggle_group');

  return React.createClass({
    displayName: 'DataTypeResultConfig',
    propTypes: {
      usesSearch: React.PropTypes.bool.isRequired,
      onChange: React.PropTypes.func.isRequired,
      isFinishedBehavior: React.PropTypes.bool.isRequired
    },

    onUseSearch: function() {
      this.props.onChange(true);
    },

    onUseList: function() {
      this.props.onChange(false);
    },

    render: function() {
      return (
        <div className="container ptxl pbxxxl">
          <div>
            <SectionHeading number="2">How to prompt the user</SectionHeading>
            <Checklist disabledWhen={this.props.isFinishedBehavior}>
              <Checklist.Item checkedWhen={!this.props.usesSearch}>
                <span>Use <span className="type-semibold">Select from a list</span> when there are few choices.</span>
                <span>The user will just pick from the whole list.</span>
              </Checklist.Item>
              <Checklist.Item checkedWhen={this.props.usesSearch}>
                <span>Use <span className="type-semibold">Search for a match</span> when there are many choices.</span>
                <span>The user will type in a search query and presented with matches to choose from.</span>
              </Checklist.Item>
            </Checklist>
          </div>

          <div>
            <ToggleGroup className="align-m">
              <ToggleGroup.Item
                title="The user will be shown a list of choices, and be asked to select one."
                label="Select from a list"
                activeWhen={!this.props.usesSearch}
                onClick={this.onUseList}
              />
              <ToggleGroup.Item
                title="The user will be asked for a search query to narrow down the choices."
                label="Search for a match"
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
