define(function(require) {
  var React = require('react'),
    FormInput = require('./input');

  return React.createClass({
    displayName: 'DayOfMonthInput',
    propTypes: {
      value: React.PropTypes.number,
      onChange: React.PropTypes.func.isRequired
    },

    onChange: function(newValue) {
      const parsed = newValue.substr(-2, 2).match(/(3[0-1]|[1-2][0-9]|[1-9])$/);
      let day;
      if (parsed) {
        day = parseInt(parsed, 10);
      }
      if (!Number.isInteger(day)) {
        day = null;
      }
      this.props.onChange(day);
    },

    getTextValue: function() {
      const day = this.props.value;
      if (Number.isInteger(day)) {
        return day.toString();
      } else {
        return "";
      }
    },

    getOrdinalSuffix: function() {
      const lastDigit = this.props.value % 10;
      const last2Digits = this.props.value % 100;
      if (lastDigit === 1 && last2Digits !== 11) {
        return "st";
      } else if (lastDigit === 2 && last2Digits !== 12) {
        return "nd";
      } else if (lastDigit === 3 && last2Digits !== 13) {
        return "rd";
      } else {
        return "th";
      }
    },

    render: function() {
      return (
        <span>
          <FormInput
            className="width-2 form-input-borderless align-c"
            value={this.getTextValue()}
            onChange={this.onChange}
          />
          <span className="align-button type-label">{this.getOrdinalSuffix()}</span>
        </span>
      );
    }
  });
});
