define(function(require) {
  var React = require('react'),
    FormInput = require('./input'),
    OptionalInt = require('../models/optional_int');

  return React.createClass({
    displayName: 'MinuteInput',
    propTypes: {
      value: React.PropTypes.number,
      onChange: React.PropTypes.func.isRequired
    },

    getTextValue: function() {
      const minute = this.props.value;
      if (Number.isInteger(minute)) {
        return minute.toString().padStart(2, "0");
      } else {
        return "";
      }
    },

    onChange: function(newValue) {
      const minutesRegex = /^([0-5]?[0-9])$/;
      const parsed = newValue.substr(-2, 2).match(minutesRegex) ||
        (newValue.substr(-3, 1) + newValue.substr(-1, 1)).match(minutesRegex);
      const minute = OptionalInt.fromString(parsed ? parsed[1] : "");
      this.props.onChange(minute.value);
    },

    render: function() {
      return (
        <span>
          <span className="align-button">:</span>
          <FormInput
            className="width-2 form-input-borderless align-c"
            value={this.getTextValue()}
            onChange={this.onChange}
          />
        </span>
      );
    }
  });
});
