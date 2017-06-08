define(function(require) {
  var React = require('react'),
    DayOfMonthEditor = require('./day_of_month_editor'),
    FrequencyEditor = require('./frequency_editor'),
    TimeOfDayEditor = require('./time_of_day_editor'),
    Recurrence = require('../models/recurrence');

  return React.createClass({
    displayName: 'MonthlyRecurrenceEditor',
    propTypes: {
      recurrence: React.PropTypes.instanceOf(Recurrence).isRequired,
      onChange: React.PropTypes.func.isRequired,
      teamTimeZone: React.PropTypes.string.isRequired
    },

    render: function() {
      return (
        <div>
          <div className="mvm">
            <FrequencyEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
              unit="month"
              units="months"
              min={1}
              max={120}
            />
          </div>
          <div className="mvm">
            <DayOfMonthEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
            />
          </div>
          <div className="mvm">
            <TimeOfDayEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
              teamTimeZone={this.props.teamTimeZone}
            />
          </div>
        </div>
      );
    }
  });
});
