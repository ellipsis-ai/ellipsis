define(function(require) {
  var React = require('react'),
    BehaviorSwitcherGroup = require('./behavior_switcher_group'),
    BehaviorVersion = require('../models/behavior_version');

  return React.createClass({
    displayName: 'BehaviorSwitcher',
    propTypes: {
      actionBehaviors: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      dataTypeBehaviors: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      selectedBehavior: React.PropTypes.instanceOf(BehaviorVersion),
      groupId: React.PropTypes.string,
      groupName: React.PropTypes.string.isRequired,
      groupDescription: React.PropTypes.string.isRequired,
      onSelectBehavior: React.PropTypes.func.isRequired,
      addNewAction: React.PropTypes.func.isRequired,
      addNewDataType: React.PropTypes.func.isRequired,
      isBehaviorModified: React.PropTypes.func.isRequired
    },

    hasSavedNameOrDescription: function() {
      return !!(this.props.groupName || this.props.groupDescription);
    },

    getEditButtonLabel: function() {
      if (this.hasSavedNameOrDescription()) {
        return "Edit title/description";
      } else {
        return "Add title/description";
      }
    },

    getSkillTitle: function() {
      return (
        <div className="mbm">
          <h4 className="mbn">{this.props.groupName}</h4>
          <div className="type-s type-weak">{this.props.groupDescription}</div>
        </div>
      );
    },

    onEditSkillDetails: function() {
      this.props.onSelectBehavior(this.props.groupId, null);
    },

    render: function() {
      return (
        <div>

          <div className="border-bottom ptl pbxl mbl">

            <div className="container container-wide">
              {this.getSkillTitle()}
            </div>

            <div className={`pvxs container container-wide ${this.props.selectedBehavior ? "" : "bg-blue border-blue-medium type-white"}`}>
              <button type="button" className="button-block" onClick={this.onEditSkillDetails} disabled={!this.props.selectedBehavior}>
                <span className={`type-s ${this.props.selectedBehavior ? "link" : "type-white"}`}>{this.getEditButtonLabel()}</span>
              </button>
            </div>
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
                isBehaviorModified={this.props.isBehaviorModified}
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
                isBehaviorModified={this.props.isBehaviorModified}
              />
            </div>
          ) : null}

        </div>
      );
    }
  });
});
