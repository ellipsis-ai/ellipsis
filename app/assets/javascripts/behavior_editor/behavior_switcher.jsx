define(function(require) {
  var React = require('react'),
    BehaviorSwitcherGroup = require('./behavior_switcher_group'),
    BehaviorVersion = require('../models/behavior_version'),
    Collapsible = require('../shared_ui/collapsible'),
    Input = require('../form/input'),
    Textarea = require('../form/textarea'),
    SVGXIcon = require('../svg/x');

  return React.createClass({
    displayName: 'BehaviorSwitcher',
    propTypes: {
      onToggle: React.PropTypes.func.isRequired,
      actionBehaviors: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      dataTypeBehaviors: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      selectedBehavior: React.PropTypes.instanceOf(BehaviorVersion),
      groupId: React.PropTypes.string,
      groupName: React.PropTypes.string.isRequired,
      lastSavedGroupName: React.PropTypes.string.isRequired,
      groupDescription: React.PropTypes.string.isRequired,
      lastSavedGroupDescription: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired,
      onBehaviorGroupNameChange: React.PropTypes.func.isRequired,
      onBehaviorGroupDescriptionChange: React.PropTypes.func.isRequired,
      onSelectBehavior: React.PropTypes.func.isRequired,
      addNewAction: React.PropTypes.func.isRequired,
      addNewDataType: React.PropTypes.func.isRequired,
      isBehaviorIdModified: React.PropTypes.func.isRequired
    },

    getInitialState: function() {
      return {
        groupName: this.props.groupName,
        groupDescription: this.props.groupDescription,
        revealSkillDetails: false
      };
    },

    getBehaviorGroupName: function() {
      return this.props.groupName;
    },

    getBehaviorGroupDescription: function() {
      return this.props.groupDescription;
    },

    hasSavedNameOrDescription: function() {
      return !!(this.props.lastSavedGroupName || this.props.lastSavedGroupDescription);
    },

    getEditButtonLabel: function() {
      if (this.hasSavedNameOrDescription()) {
        return "Edit title/description";
      } else {
        return "Add title/description";
      }
    },

    getSkillName: function() {
      var optionalName = this.props.lastSavedGroupName;
      if (optionalName) {
        return (
          <span className="type-black">{optionalName}</span>
        );
      } else if (this.props.groupId) {
        return 'Untitled skill';
      } else {
        return 'New skill';
      }
    },

    getSkillTitle: function() {
      return (
        <div className="mbm">
          <h4 className="mbn">{this.getSkillName()}</h4>
          <div className="type-s type-weak">{this.props.lastSavedGroupDescription}</div>
        </div>
      );
    },

    toggleSkillDetails: function() {
      this.setState({ revealSkillDetails: !this.state.revealSkillDetails }, () => {
        if (this.state.revealSkillDetails) {
          this.refs.skillName.focus();
        }
      });
    },

    focus: function() {
      this.refs.closeButton.focus();
    },

    render: function() {
      return (
        <div className="position-relative" ref="behaviorSwitcher">
          <div className="position-absolute position-top-right ptxs prxs type-weak">
            <button ref="closeButton" type="button" className="button-symbol button-s button-subtle" onClick={this.props.onToggle}><SVGXIcon /></button>
          </div>

          <div className="border-bottom ptl pbxl mbl container container-wide">

            <Collapsible revealWhen={!this.state.revealSkillDetails}>
              {this.getSkillTitle()}

              <button type="button" className="button-s button-shrink" onClick={this.toggleSkillDetails}>
                {this.getEditButtonLabel()}
              </button>
            </Collapsible>
            <Collapsible revealWhen={this.state.revealSkillDetails}>
              <h5>Skill details</h5>
              <p className="type-s type-weak">
                <span>A title and description informs your team the general purpose of </span>
                <span>this skill. </span>
              </p>

              <div className="mbs">
                <Input
                  ref="skillName"
                  className="form-input-borderless type-bold mbn"
                  placeholder="Short title"
                  onChange={this.props.onBehaviorGroupNameChange}
                  value={this.getBehaviorGroupName()}
                />
              </div>
              <div className="mbs">
                <Textarea
                  className="form-input-height-auto form-input-borderless mbn"
                  placeholder="Description (optional)"
                  onChange={this.props.onBehaviorGroupDescriptionChange}
                  value={this.getBehaviorGroupDescription()}
                  rows={"3"}
                />
              </div>
            </Collapsible>
          </div>

          {this.props.groupId ? (
            <div>
              <BehaviorSwitcherGroup
                ref="actionSwitcher"
                heading="Actions"
                behaviors={this.props.actionBehaviors}
                selectedBehavior={this.props.selectedBehavior}
                onAddNew={this.props.addNewAction}
                addNewLabel="Add new action"
                emptyMessage="Add actions to provide a response using custom data types for input."
                onSelectBehavior={this.props.onSelectBehavior}
                isBehaviorIdModified={this.props.isBehaviorIdModified}
              />

              <BehaviorSwitcherGroup
                ref="dataTypeSwitcher"
                heading="Data types"
                behaviors={this.props.dataTypeBehaviors}
                selectedBehavior={this.props.selectedBehavior}
                onAddNew={this.props.addNewDataType}
                addNewLabel="Add new data type"
                emptyMessage="Custom data types allow you to limit user input to a set of choices, backed by custom data."
                onSelectBehavior={this.props.onSelectBehavior}
                isBehaviorIdModified={this.props.isBehaviorIdModified}
              />
            </div>
          ) : null}

        </div>
      );
    }
  });
});
