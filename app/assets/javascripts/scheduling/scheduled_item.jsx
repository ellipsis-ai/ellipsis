define(function(require) {
  var React = require('react'),
    Formatter = require('../lib/formatter'),
    BehaviorGroup = require('../models/behavior_group'),
    ScheduledAction = require('../models/scheduled_action'),
    ScheduledItemTitle = require('./scheduled_item_title');

  return React.createClass({
    displayName: 'ScheduledItem',
    propTypes: {
      scheduledAction: React.PropTypes.instanceOf(ScheduledAction).isRequired,
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)).isRequired,
      onClick: React.PropTypes.func.isRequired
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

    toggle: function() {
      this.props.onClick(this.props.scheduledAction);
    },

    render: function() {
      return (
        <div className="type-s">
          <button type="button" className="button-block" onClick={this.toggle}>
            <span>
              <span><ScheduledItemTitle scheduledAction={this.props.scheduledAction} behaviorGroups={this.props.behaviorGroups} /> </span>
              <span className="link">{this.getRecurrenceSummary()}</span>
            </span>
          </button>
          <div>
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
