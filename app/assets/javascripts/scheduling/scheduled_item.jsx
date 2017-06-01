define(function(require) {
  var React = require('react'),
    Formatter = require('../lib/formatter'),
    ScheduledAction = require('../models/scheduled_action');

  return React.createClass({
    displayName: 'ScheduledItem',
    propTypes: {
      scheduledAction: React.PropTypes.instanceOf(ScheduledAction).isRequired
    },

    getTriggerText: function() {
      return this.props.scheduledAction.trigger || "";
    },

    hasTriggerText: function() {
      return !!this.props.scheduledAction.trigger;
    },

    getActionName: function() {
      return this.props.scheduledAction.behaviorName || "an unnamed action";
    },

    getSkillName: function() {
      return this.props.scheduledAction.behaviorGroupName || "";
    },

    hasSkillName: function() {
      return !!this.props.scheduledAction.behaviorGroupName;
    },

    getRecurrenceSummary: function() {
      return this.props.scheduledAction.recurrence.displayString;
    },

    getFirstRecurrence: function() {
      return this.props.scheduledAction.firstRecurrence;
    },

    getSecondRecurrence: function() {
      return this.props.scheduledAction.secondRecurrence;
    },

    renderTriggerTitle: function() {
      return (
        <span className="">
          <span>Run </span>
          <span className="box-chat mlxs mrs type-black">{this.getTriggerText()}</span>
          <span> {this.getRecurrenceSummary()}</span>
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
          <span> {this.getRecurrenceSummary()}</span>
        </span>
      );
    },

    renderTitle: function() {
      if (this.hasTriggerText()) {
        return this.renderTriggerTitle();
      } else {
        return this.renderActionNameTitle();
      }
    },

    render: function() {
      return (
        <div className="type-s">
          <div>{this.renderTitle()}</div>
          <div className="mtl">
            <h5>Next two times:</h5>
            <div>
              {Formatter.formatTimestampLong(this.getFirstRecurrence())}
            </div>
            <div>
              {Formatter.formatTimestampLong(this.getSecondRecurrence())}
            </div>
          </div>
        </div>
      );
    }
  });
});
