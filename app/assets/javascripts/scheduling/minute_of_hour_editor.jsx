define(function(require) {
  var React = require('react'),
    MinuteInput = require('../form/minute_input'),
    Recurrence = require('../models/recurrence');

  return React.createClass({
    displayName: 'MinuteOfHourEditor',
    propTypes: {
      recurrence: React.PropTypes.instanceOf(Recurrence).isRequired,
      onChange: React.PropTypes.func.isRequired
    },

    getValue: function() {
      return this.props.recurrence.minuteOfHour;
    },

    onChange: function(newValue) {
      this.props.onChange(this.props.recurrence.clone({
        minuteOfHour: newValue
      }));
    },

    getSuffix: function() {
      const value = this.getValue();
      if (!value) {
        return "(on the hour)";
      } else if (value === 1) {
        return "minute past the hour";
      } else {
        return "minutes past the hour";
      }
    },

    render: function() {
      return (
        <div>
          <span className="align-button mrm">At</span>
          <MinuteInput value={this.getValue()} onChange={this.onChange} />
          <span className="align-button mlm">{this.getSuffix()}</span>
        </div>
      );
    }
  });
});
