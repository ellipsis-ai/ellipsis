define(function(require) {
  var React = require('react'),
    SectionHeading = require('./section_heading'),
    Checklist = require('./checklist'),
    ToggleGroup = require('../form/toggle_group');

  return React.createClass({
    displayName: 'DataTypeResultConfig',
    propTypes: {
      usesSearch: React.PropTypes.bool.isRequired,
      onChange: React.PropTypes.func.isRequired
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
          <div className="columns">
            <div className="column column-one-quarter mobile-column-full mts mbxxl mobile-mbs">
              <SectionHeading>How to prompt the user</SectionHeading>
              <Checklist disabledWhen={false}>
                <Checklist.Item checkedWhen={!this.props.usesSearch}>
                  <span>Use <code className="type-bold">Select from a list</code> when there are few choices.</span>
                  <span>The user will just pick from the whole list.</span>
                </Checklist.Item>
                <Checklist.Item checkedWhen={this.props.usesSearch}>
                  <span>Use <code className="type-bold">Search for a match</code> when there are many choices.</span>
                  <span>The user will type in a search query and presented with matches to choose from.</span>
                </Checklist.Item>
              </Checklist>
            </div>
            <div className="column column-three-quarters mobile-column-full pll mobile-pln mbxxl">
              <div className="mbm">
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
          </div>
        </div>
      );
    }
  });
});
