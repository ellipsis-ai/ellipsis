define(function(require) {
  var React = require('react'),
    FrequencyEditor = require('./frequency_editor'),
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
          <FrequencyEditor
            recurrence={this.props.recurrence}
            onChange={this.props.onChange}
            unit="hour"
            units="hours"
            min={1}
            max={8760}
          />
        </div>
      );
    }
  });
});
