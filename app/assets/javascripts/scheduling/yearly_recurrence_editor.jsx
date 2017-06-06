define(function(require) {
  var React = require('react'),
    FrequencyEditor = require('./frequency_editor'),
    Recurrence = require('../models/recurrence');

  return React.createClass({
    displayName: 'YearlyRecurrenceEditor',
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
              unit="year"
              units="years"
              min={1}
              max={10}
            />
          </div>
        </div>
      );
    }
  });
});
