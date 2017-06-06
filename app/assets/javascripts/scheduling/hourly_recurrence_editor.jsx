define(function(require) {
  var React = require('react'),
    FrequencyEditor = require('./frequency_editor'),
    MinuteOfHourEditor = require('./minute_of_hour_editor'),
    Recurrence = require('../models/recurrence');

  return React.createClass({
    displayName: 'HourlyRecurrenceEditor',
    propTypes: {
      recurrence: React.PropTypes.instanceOf(Recurrence).isRequired,
      onChange: React.PropTypes.func.isRequired
    },

    render: function() {
      return (
        <div>
          <div className="mvl">
            <FrequencyEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
              unit="hour"
              units="hours"
              min={1}
              max={8760}
            />
          </div>
          <div className="mvl">
            <MinuteOfHourEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
            />
          </div>
        </div>
      );
    }
  });
});
