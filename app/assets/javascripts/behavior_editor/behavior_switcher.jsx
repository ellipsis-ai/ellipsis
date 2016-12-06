define(function(require) {
  var React = require('react'),
    BehaviorSwitcherGroup = require('./behavior_switcher_group'),
    BehaviorVersion = require('../models/behavior_version'),
    Collapsible = require('../collapsible'),
    Input = require('../form/input'),
    SVGXIcon = require('../svg/x'),
    ifPresent = require('../if_present');

  return React.createClass({
    displayName: 'BehaviorSwitcher',
    propTypes: {
      onToggle: React.PropTypes.func.isRequired,
      actionBehaviors: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      dataTypeBehaviors: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      currentBehavior: React.PropTypes.instanceOf(BehaviorVersion),
      groupId: React.PropTypes.string.isRequired,
      groupName: React.PropTypes.string.isRequired,
      lastSavedGroupName: React.PropTypes.string.isRequired,
      groupDescription: React.PropTypes.string.isRequired,
      lastSavedGroupDescription: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired,
      onBehaviorGroupNameChange: React.PropTypes.func.isRequired,
      onBehaviorGroupDescriptionChange: React.PropTypes.func.isRequired,
      onSaveBehaviorGroupDetails: React.PropTypes.func.isRequired,
      onCancelBehaviorGroupDetails: React.PropTypes.func.isRequired
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

    saveNameAndDescription: function() {
      if (this.groupDetailsHaveUnsavedChanges()) {
        this.props.onSaveBehaviorGroupDetails();
      }
      this.toggleSkillDetails();
    },

    cancelNameAndDescription: function() {
      this.props.onCancelBehaviorGroupDetails();
      this.toggleSkillDetails();
    },

    hasSavedNameOrDescription: function() {
      return !!(this.props.lastSavedGroupName || this.props.lastSavedGroupDescription);
    },

    groupDetailsHaveUnsavedChanges: function() {
      return this.props.groupName !== this.props.lastSavedGroupName ||
        this.props.groupDescription !== this.props.lastSavedGroupDescription;
    },

    isSimpleSkill: function() {
      return !this.hasMultipleActions() && !this.hasDataTypes();
    },

    hasMultipleActions: function() {
      return this.props.actionBehaviors.length > 1;
    },

    hasDataTypes: function() {
      return this.props.dataTypeBehaviors.length > 1;
    },

    getEditButtonLabel: function() {
      if (this.hasSavedNameOrDescription()) {
        return "Edit name/description";
      } else {
        return "Add name/description";
      }
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

    renderIntro: function() {
      if (this.isSimpleSkill()) {
        return (
          <p className="type-weak">
            <span>This skill has one action. You can add additional actions to group related </span>
            <span>responses together, and re-use saved answers. You can also add custom data types.</span>
          </p>
        );
      } else if (this.hasMultipleActions()) {
        return (
          <p className="type-weak">
            <span>This skill groups multiple related actions, each providing a different response to certain triggers. </span>
            <span>Actions share custom data types, and can re-use saved answers.</span>
          </p>
        );
      } else if (this.hasDataTypes()) {
        return (
          <p className="type-weak">
            <span>This skill includes a single action with custom data types for user input. You can add additional related actions </span>
            <span>to group them together, and to re-use saved answers.</span>
          </p>
        );
      }
    },

    render: function() {
      return (
        <div className="position-relative width-30 mobile-width-full" ref="behaviorSwitcher">
          <div className="align-r ptxs prxs">
            <button ref="closeButton" type="button" className="button-symbol button-s button-subtle" onClick={this.props.onToggle}><SVGXIcon /></button>
          </div>
          <div className="container container-wide mbxl">
            {this.renderIntro()}
          </div>

          <BehaviorSwitcherGroup
            ref="actionSwitcher"
            heading="Actions"
            behaviors={this.props.actionBehaviors}
            currentBehavior={this.props.currentBehavior}
            addNewUrl={jsRoutes.controllers.BehaviorEditorController.newForNormalBehavior(this.props.groupId, this.props.teamId).url}
            addNewLabel="Add new action"
            emptyMessage="Add actions to provide a response using custom data types for input."
          />
          <BehaviorSwitcherGroup
            ref="dataTypeSwitcher"
            heading="Custom data types"
            behaviors={this.props.dataTypeBehaviors}
            currentBehavior={this.props.currentBehavior}
            addNewUrl={jsRoutes.controllers.BehaviorEditorController.newForDataType(this.props.groupId, this.props.teamId).url}
            addNewLabel="Add new data type"
            emptyMessage="Custom data types allow you to limit user input to a set of choices, backed by custom data."
          />

          <div className="mtxl mbxxxl container container-wide">
            <div className="columns columns-elastic">
              <div className="column column-expand">
                <h4>Skill details</h4>
              </div>
              <div className="column column-shrink">
                <Collapsible revealWhen={!this.state.revealSkillDetails}>
                  <button type="button" className="button-s button-shrink" onClick={this.toggleSkillDetails}>
                    {this.getEditButtonLabel()}
                  </button>
                </Collapsible>
              </div>
            </div>
            <Collapsible revealWhen={!this.state.revealSkillDetails}>
              <div className="type-s">
                {ifPresent(this.getBehaviorGroupName(), (name) => (
                  <b>{name}</b>
                ), () => (
                  <span className="type-weak">Untitled skill</span>
                ))}
                {ifPresent(this.getBehaviorGroupDescription(), (desc) => (
                  <span className="type-weak"> · {desc}</span>
                ))}
              </div>
            </Collapsible>

            <Collapsible revealWhen={this.state.revealSkillDetails}>
              <p className="type-s type-weak">
                <span>A title and description informs your team the general purpose of </span>
                <span>this skill. </span>
                {this.hasMultipleActions() ? null : (
                  <span>(This is optional, and more useful for skills that have multiple actions.)</span>
                )}
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
                <Input
                  className="form-input-borderless mbn"
                  placeholder="Description (optional)"
                  onChange={this.props.onBehaviorGroupDescriptionChange}
                  value={this.getBehaviorGroupDescription()}
                />
              </div>
              <div className="mtxl">
                <button type="button"
                  onClick={this.saveNameAndDescription}
                  className="button-s button-primary mrs mbs"
                  disabled={!this.groupDetailsHaveUnsavedChanges()}
                >
                  Save
                </button>
                <button type="button" onClick={this.cancelNameAndDescription} className="button-s mbs">
                  Cancel
                </button>
              </div>
            </Collapsible>
          </div>
        </div>
      );
    }
  });
});
