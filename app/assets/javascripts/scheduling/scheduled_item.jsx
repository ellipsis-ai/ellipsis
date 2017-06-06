define(function(require) {
  var React = require('react'),
    Formatter = require('../lib/formatter'),
    ScheduledAction = require('../models/scheduled_action'),
    ScheduledItemTitle = require('./scheduled_item_title');

  return React.createClass({
    displayName: 'ScheduledItem',
    propTypes: {
      scheduledAction: React.PropTypes.instanceOf(ScheduledAction).isRequired,
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
              <ScheduledItemTitle scheduledAction={this.props.scheduledAction} />
              <span> {this.getRecurrenceSummary()}</span>
            </span>
          </button>
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
