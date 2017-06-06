define(function(require) {
  var React = require('react'),
    ScheduledAction = require('../models/scheduled_action');

  return React.createClass({
    displayName: 'ScheduledItemTitle',
    propTypes: {
      scheduledAction: React.PropTypes.instanceOf(ScheduledAction).isRequired
    },

    hasSkillName: function() {
      return !!this.props.scheduledAction.behaviorGroupName;
    },

    getSkillName: function() {
      return this.props.scheduledAction.behaviorGroupName || "";
    },

    getActionName: function() {
      return this.props.scheduledAction.behaviorName || "an unnamed action";
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
      return (
        <span className="">
          <span>Run </span>
          <span className="border phxs mhxs type-black bg-white">{this.getActionName()}</span>
          {this.hasSkillName() ? (
            <span>
              <span> in the </span>
              <span className="border phxs mhxs type-black bg-white">{this.getSkillName()}</span>
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
