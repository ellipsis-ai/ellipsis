define(function(require) {
  var React = require('react'),
    FrequencyEditor = require('./frequency_editor'),
    Recurrence = require('../models/recurrence');

  return React.createClass({
    displayName: 'DailyRecurrenceEditor',
    propTypes: {
      recurrence: React.PropTypes.instanceOf(Recurrence).isRequired,
      onChange: React.PropTypes.func.isRequired
    },

    render: function() {
      return (
        <div>
          <FrequencyEditor
            recurrence={this.props.recurrence}
            onChange={this.props.onChange}
            unit="day"
            units="days"
            min={1}
            max={3650}
          />
        </div>
      );
    }
  });
});
