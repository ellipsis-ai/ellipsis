define(function(require) {
  var React = require('react'),
    BehaviorSwitcherGroup = require('./behavior_switcher_group'),
    BehaviorVersion = require('../models/behavior_version'),
    SVGXIcon = require('../svg/x');

  return React.createClass({
    displayName: 'BehaviorSwitcher',
    propTypes: {
      onToggle: React.PropTypes.func.isRequired,
      actionBehaviors: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      dataTypeBehaviors: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      currentBehavior: React.PropTypes.instanceOf(BehaviorVersion),
      groupId: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired
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
        <div className="position-relative width-20" ref="behaviorSwitcher">
          <div className="align-r ptxs prxs">
            <button type="button" className="button-symbol button-s button-subtle" onClick={this.props.onToggle}><SVGXIcon /></button>
          </div>
          <BehaviorSwitcherGroup
            ref="actionSwitcher"
            heading={this.getActionsHeading()}
            behaviors={this.props.actionBehaviors}
            currentBehavior={this.props.currentBehavior}
            addNewUrl={jsRoutes.controllers.BehaviorEditorController.newForNormalBehavior(this.props.groupId, this.props.teamId).url}
            addNewLabel="Add a new action"
          />
          <BehaviorSwitcherGroup
            ref="dataTypeSwitcher"
            heading={this.getDataTypesHeading()}
            behaviors={this.props.dataTypeBehaviors}
            currentBehavior={this.props.currentBehavior}
            addNewUrl={jsRoutes.controllers.BehaviorEditorController.newForDataType(this.props.groupId, this.props.teamId).url}
            addNewLabel="Add a new data type"
          />
        </div>
      );
    }
  });
});
