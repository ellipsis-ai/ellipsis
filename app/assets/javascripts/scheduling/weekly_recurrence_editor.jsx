define(function(require) {
  var React = require('react'),
    FrequencyEditor = require('./frequency_editor'),
    TimeOfDayEditor = require('./time_of_day_editor'),
    WeekdayEditor = require('./weekday_editor'),
    Recurrence = require('../models/recurrence');

  return React.createClass({
    displayName: 'WeeklyRecurrenceEditor',
    propTypes: {
      recurrence: React.PropTypes.instanceOf(Recurrence).isRequired,
      onChange: React.PropTypes.func.isRequired,
      teamTimeZone: React.PropTypes.string.isRequired,
      teamTimeZoneName: React.PropTypes.string.isRequired
    },

    render: function() {
      return (
        <div>
          <div className="mvm">
            <FrequencyEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
              unit="week"
              units="weeks"
              min={1}
              max={520}
            />
          </div>
          <div className="mvm">
            <WeekdayEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
            />
          </div>
          <div className="mvm">
            <TimeOfDayEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
              teamTimeZone={this.props.teamTimeZone}
              teamTimeZoneName={this.props.teamTimeZoneName}
            />
          </div>
        </div>
      );
    }
  });
});
