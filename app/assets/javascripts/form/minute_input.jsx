define(function(require) {
  var React = require('react'),
    FormInput = require('./input');

  return React.createClass({
    displayName: 'MinuteInput',
    propTypes: {
      value: React.PropTypes.number,
      onChange: React.PropTypes.func.isRequired
    },

    getTextValue: function() {
      const minute = this.props.value;
      if (typeof minute === "number") {
        return minute.toString().padStart(2, "0");
      } else {
        return "";
      }
    },

    onChange: function(newValue) {
      const minutesRegex = /^([0-5]?[0-9])$/;
      const parsed = newValue.substr(-2, 2).match(minutesRegex) ||
        (newValue.substr(-3, 1) + newValue.substr(-1, 1)).match(minutesRegex);
      let minute;
      if (parsed) {
        minute = parseInt(parsed[1], 10);
      }
      if (isNaN(minute)) {
        minute = null;
      }
      this.props.onChange(minute);
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
