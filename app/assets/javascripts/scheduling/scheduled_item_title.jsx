define(function(require) {
  var React = require('react'),
    BehaviorGroup = require('../models/behavior_group'),
    ScheduledAction = require('../models/scheduled_action');

  return React.createClass({
    displayName: 'ScheduledItemTitle',
    propTypes: {
      scheduledAction: React.PropTypes.instanceOf(ScheduledAction).isRequired,
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)).isRequired
    },

    getTriggerText: function() {
      return this.props.scheduledAction.trigger || "";
    },

    hasTriggerText: function() {
      return !!this.props.scheduledAction.trigger;
    },

    renderTriggerTitle: function() {
      return (
        <span>
          <span>Run </span>
          <span className="box-chat mhs type-black">{this.getTriggerText()}</span>
        </span>
      );
    },

    renderActionNameTitle: function() {
      const skillName = this.props.scheduledAction.getSkillNameFromGroups(this.props.behaviorGroups);
      const actionName = this.props.scheduledAction.getActionNameFromGroups(this.props.behaviorGroups);
      return (
        <span className="">
          <span>Run </span>
          <span className="border phxs mhxs type-black bg-white">{actionName || "an unnamed action"}</span>
          {skillName ? (
            <span>
              <span> in the </span>
              <span className="border phxs mhxs type-black bg-white">{skillName}</span>
              <span> skill</span>
            </span>
          ) : null}
        </span>
      );
    },

    render: function() {
      if (this.hasTriggerText()) {
        return this.renderTriggerTitle();
      } else {
        return this.renderActionNameTitle();
      }
    }
  });
});
