define(function(require) {
  var React = require('react'),
    FrequencyEditor = require('./frequency_editor'),
    TimeOfDayEditor = require('./time_of_day_editor'),
    Recurrence = require('../models/recurrence');

  return React.createClass({
    displayName: 'YearlyRecurrenceEditor',
    propTypes: {
      recurrence: React.PropTypes.instanceOf(Recurrence).isRequired,
      onChange: React.PropTypes.func.isRequired,
      teamTimeZone: React.PropTypes.string.isRequired
    },

    render: function() {
      return (
        <div>
          <div className="mvl">
            <FrequencyEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
              unit="year"
              units="years"
              min={1}
              max={10}
            />
          </div>
          <div className="mvl">
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
