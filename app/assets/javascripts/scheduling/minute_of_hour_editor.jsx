define(function(require) {
  var React = require('react'),
    FormInput = require('../form/input'),
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

    getTextValue: function() {
      const minute = this.getValue() || 0;
      let str = minute.toString().padStart(2, "0");
      return `:${str}`;
    },

    getValidInt: function(int) {
      if (isNaN(int) || int < 0) {
        return 0;
      } else if (int > 59) {
        return 59;
      } else {
        return int;
      }
    },

    onChange: function(newValue) {
      const stripped = newValue.replace(/^:/, '').substr(-2, 2);
      const newMinute = this.getValidInt(parseInt(stripped, 10));
      this.props.onChange(this.props.recurrence.clone({
        minuteOfHour: newMinute
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
          <FormInput
            className="width-5 form-input-borderless align-c"
            value={this.getTextValue()}
            onChange={this.onChange}
          />
          <span className="align-button mlm">{this.getSuffix()}</span>
        </div>
      );
    }
  });
});
