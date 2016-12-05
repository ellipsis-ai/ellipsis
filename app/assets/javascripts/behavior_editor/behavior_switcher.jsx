define(function(require) {
  var React = require('react'),
    BehaviorSwitcherGroup = require('./behavior_switcher_group'),
    BehaviorVersion = require('../models/behavior_version'),
    Input = require('../form/input'),
    SVGXIcon = require('../svg/x');

  return React.createClass({
    displayName: 'BehaviorSwitcher',
    propTypes: {
      onToggle: React.PropTypes.func.isRequired,
      actionBehaviors: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      dataTypeBehaviors: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      currentBehavior: React.PropTypes.instanceOf(BehaviorVersion),
      groupId: React.PropTypes.string.isRequired,
      groupName: React.PropTypes.string.isRequired,
      groupDescription: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired,
      onBehaviorGroupNameChange: React.PropTypes.func.isRequired,
      onBehaviorGroupDescriptionChange: React.PropTypes.func.isRequired,
      onSaveBehaviorGroupName: React.PropTypes.func.isRequired,
      onSaveBehaviorGroupDescription: React.PropTypes.func.isRequired
    },

    getInitialState: function() {
      return {
        groupName: this.props.groupName,
        groupDescription: this.props.groupDescription
      };
    },

    getBehaviorGroupName: function() {
      return this.props.groupName;
    },

    getBehaviorGroupDescription: function() {
      return this.props.groupDescription;
    },

    render: function() {
      return (
        <div className="position-relative width-30 mobile-width-full" ref="behaviorSwitcher">
          <div className="align-r ptxs prxs">
            <button type="button" className="button-symbol button-s button-subtle" onClick={this.props.onToggle}><SVGXIcon /></button>
          </div>
          <div className="container container-wide mbxl">
            <div className="mbs">
              <Input
                className="form-input-borderless form-input-l type-bold mbn"
                placeholder="Skill name (optional)"
                onChange={this.props.onBehaviorGroupNameChange}
                onBlur={this.props.onSaveBehaviorGroupName}
                onEnter={this.props.onSaveBehaviorGroupName}
                value={this.getBehaviorGroupName()}
              />
            </div>
            <div className="mbs">
              <Input
                className="form-input-borderless form-input-m mbn"
                placeholder="Description (optional)"
                onChange={this.props.onBehaviorGroupDescriptionChange}
                onBlur={this.props.onSaveBehaviorGroupDescription}
                onEnter={this.props.onSaveBehaviorGroupDescription}
                value={this.getBehaviorGroupDescription()}
              />
            </div>

            <p className="type-s type-weak mtl">
              <span>Skills may include one or more related actions, each providing a response to certain triggers. </span>
              <span>Actions can share user input and data types.</span>
            </p>
          </div>

          <BehaviorSwitcherGroup
            ref="actionSwitcher"
            heading="Actions"
            behaviors={this.props.actionBehaviors}
            currentBehavior={this.props.currentBehavior}
            addNewUrl={jsRoutes.controllers.BehaviorEditorController.newForNormalBehavior(this.props.groupId, this.props.teamId).url}
            addNewLabel="Add new action"
            emptyMessage="No actions in this skill"
          />
          <BehaviorSwitcherGroup
            ref="dataTypeSwitcher"
            heading="Custom data types"
            behaviors={this.props.dataTypeBehaviors}
            currentBehavior={this.props.currentBehavior}
            addNewUrl={jsRoutes.controllers.BehaviorEditorController.newForDataType(this.props.groupId, this.props.teamId).url}
            addNewLabel="Add new data type"
            emptyMessage="No data types in this skill"
          />
        </div>
      );
    }
  });
});
