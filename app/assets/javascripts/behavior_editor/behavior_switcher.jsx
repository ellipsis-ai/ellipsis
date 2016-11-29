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
      csrfToken: React.PropTypes.string.isRequired
    },

    getInitialState: function() {
      return {
        groupName: this.props.groupName,
        groupDescription: this.props.groupDescription
      };
    },

    getActionsHeading: function() {
      const count = this.props.actionBehaviors.length;
      if (count === 0) {
        return 'Skill actions';
      } else if (count === 1) {
        return '1 action in this skill';
      } else {
        return `${count} actions in this skill`;
      }
    },

    postOptions: function(data) {
      return {
        credentials: 'same-origin',
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
          'Csrf-Token': this.props.csrfToken
        },
        body: JSON.stringify(data)
      };
    },

    getBehaviorGroupName: function() {
      return this.state.groupName;
    },

    onBehaviorGroupNameChange: function(name) {
      this.setState({
        groupName: name
      });
    },

    saveBehaviorGroupName: function(name) {
      var url = jsRoutes.controllers.BehaviorEditorController.saveBehaviorGroupName().url;
      var data = {
        groupId: this.props.groupId,
        name: name
      };
      fetch(url, this.postOptions(data)).then(() => this.onBehaviorGroupNameChange.bind(this, name));
    },

    getBehaviorGroupDescription: function() {
      return this.state.groupDescription;
    },

    onBehaviorGroupDescriptionChange: function(desc) {
      this.setState({
        groupDescription: desc
      });
    },

    saveBehaviorGroupDescription: function(desc) {
      var url = jsRoutes.controllers.BehaviorEditorController.saveBehaviorGroupDescription().url;
      var data = {
        groupId: this.props.groupId,
        description: desc
      };
      fetch(url, this.postOptions(data)).then(() => this.onBehaviorGroupDescriptionChange.bind(this, name));
    },

    getDataTypesHeading: function() {
      const count = this.props.dataTypeBehaviors.length;
      if (count === 0) {
        return 'Custom data types';
      } else if (count === 1) {
        return '1 custom data type';
      } else {
        return `${count} custom data types`;
      }
    },

    render: function() {
      return (
        <div className="position-relative width-30 mobile-width-full" ref="behaviorSwitcher">
          <div className="align-r ptxs prxs">
            <button type="button" className="button-symbol button-s button-subtle" onClick={this.props.onToggle}><SVGXIcon /></button>
          </div>
          <div className="phl mbxl">
            <div className="mbs">
              <Input
                className="form-input-borderless form-input-l type-bold mbn"
                placeholder="Skill name (optional)"
                onChange={this.onBehaviorGroupNameChange}
                onBlur={this.saveBehaviorGroupName}
                onEnter={this.saveBehaviorGroupName}
                value={this.getBehaviorGroupName()}
              />
            </div>
            <div className="mbs">
              <Input
                className="form-input-borderless form-input-m mbn"
                placeholder="Description (optional)"
                onChange={this.onBehaviorGroupDescriptionChange}
                onBlur={this.saveBehaviorGroupDescription}
                onEnter={this.saveBehaviorGroupDescription}
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
            heading={this.getActionsHeading()}
            behaviors={this.props.actionBehaviors}
            currentBehavior={this.props.currentBehavior}
            addNewUrl={jsRoutes.controllers.BehaviorEditorController.newForNormalBehavior(this.props.groupId, this.props.teamId).url}
            addNewLabel="Add new action"
            emptyMessage="No actions in this skill"
          />
          <BehaviorSwitcherGroup
            ref="dataTypeSwitcher"
            heading={this.getDataTypesHeading()}
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
