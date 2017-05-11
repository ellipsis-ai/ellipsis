define(function(require) {
  var React = require('react'),
    BehaviorSwitcherGroup = require('./behavior_switcher_group'),
    BehaviorVersion = require('../models/behavior_version'),
    LibraryVersion = require('../models/library_version');

  return React.createClass({
    displayName: 'BehaviorSwitcher',
    propTypes: {
      actionBehaviors: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      dataTypeBehaviors: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      libraries: React.PropTypes.arrayOf(React.PropTypes.instanceOf(LibraryVersion)).isRequired,
      selectedId: React.PropTypes.string,
      groupId: React.PropTypes.string,
      groupName: React.PropTypes.string.isRequired,
      groupDescription: React.PropTypes.string.isRequired,
      onSelect: React.PropTypes.func.isRequired,
      addNewAction: React.PropTypes.func.isRequired,
      addNewDataType: React.PropTypes.func.isRequired,
      isBehaviorModified: React.PropTypes.func.isRequired
    },

    getSkillTitle: function() {
      return (
        <div className="mbxs">
          <h4 className="mbn">{this.props.groupName}</h4>
          <div className="type-s type-weak display-ellipsis display-limit-width"
            title={this.props.groupDescription}>
            {this.props.groupDescription}
          </div>
        </div>
      );
    },

    onEditSkillDetails: function() {
      this.props.onSelect(this.props.groupId, null);
    },

    getAllBehaviors: function() {
      return this.props.actionBehaviors.concat(this.props.dataTypeBehaviors);
    },

    getSelectedBehavior: function() {
      return this.getAllBehaviors().find(ea => ea.behaviorId === this.props.selectedId);
    },

    getSelectedLibrary: function() {
      return this.props.libraries.find(ea => ea.libraryId === this.props.selectedId);
    },

    render: function() {
      return (
        <div className="pbxxxl">

          <div className="border-bottom ptxl pbl mbl">

            <div className="container container-wide">
              {this.getSkillTitle()}
            </div>

            <div>
              <button type="button"
                className={
                  "button-block pvxs phxl mobile-phl width width-full " +
                  (this.getSelectedBehavior() ? "" : "bg-blue border-blue-medium type-white")
                }
                onClick={this.onEditSkillDetails}
                disabled={!this.getSelectedBehavior()}
              >
                <span className={`type-s ${this.getSelectedBehavior() ? "link" : "type-white"}`}>Skill details</span>
              </button>
            </div>
          </div>

          {this.props.groupId ? (
            <div>
              <BehaviorSwitcherGroup
                ref="actionSwitcher"
                heading="Actions"
                behaviors={this.props.actionBehaviors}
                selectedBehavior={this.getSelectedBehavior()}
                onAddNew={this.props.addNewAction}
                addNewLabel="Add new action"
                emptyMessage="Add actions to provide a response using custom data types for input."
                onSelect={this.props.onSelect}
                isBehaviorModified={this.props.isBehaviorModified}
              />

              <BehaviorSwitcherGroup
                ref="dataTypeSwitcher"
                heading="Data types"
                behaviors={this.props.dataTypeBehaviors}
                selectedBehavior={this.getSelectedBehavior()}
                onAddNew={this.props.addNewDataType}
                addNewLabel="Add new data type"
                emptyMessage="Custom data types allow you to limit user input to a set of choices, backed by custom data."
                onSelect={this.props.onSelect}
                isBehaviorModified={this.props.isBehaviorModified}
              />

              <BehaviorSwitcherGroup
                ref="librarySwitcher"
                heading="Libraries"
                behaviors={this.props.libraries}
                selectedBehavior={this.getSelectedLibrary()}
                onAddNew={this.props.addNewLibrary}
                addNewLabel="Add new library"
                emptyMessage="Libraries are shareable bits of code that you can require() from elsewhere in the skill"
                onSelect={this.props.onSelect}
                isBehaviorModified={this.props.isBehaviorModified}
              />
            </div>
          ) : null}

        </div>
      );
    }
  });
});
