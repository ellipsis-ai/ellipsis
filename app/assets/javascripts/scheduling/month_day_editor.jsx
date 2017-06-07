define(function(require) {
  var React = require('react'),
    DayOfMonthInput = require('../form/day_of_month_input'),
    Select = require('../form/select'),
    Recurrence = require('../models/recurrence');

  const MONTH_NAMES = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
  const MONTH_ENUM_VALUES = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];
  const MAX_DAYS = [31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];

  return React.createClass({
    displayName: 'MonthDayEditor',
    propTypes: {
      recurrence: React.PropTypes.instanceOf(Recurrence).isRequired,
      onChange: React.PropTypes.func.isRequired
    },

    getDay: function() {
      return this.props.recurrence.dayOfMonth;
    },

    getMonth: function() {
      return this.props.recurrence.month;
    },

    getMonthText: function() {
      return Number.isInteger(this.props.recurrence.month) ? this.props.recurrence.month.toString() : "";
    },

    getMaxDayForMonth: function(monthEnumValue) {
      return MAX_DAYS[monthEnumValue - 1] || 31;
    },

    limitMaxDayForMonth: function(newMonth, newDay) {
      if (Number.isInteger(newDay) && Number.isInteger(newMonth)) {
        return Math.min(newDay, this.getMaxDayForMonth(newMonth));
      } else {
        return newDay;
      }
    },

    onChangeDay: function(newDay) {
      this.props.onChange(this.props.recurrence.clone({
        dayOfMonth: this.limitMaxDayForMonth(this.getMonth(), newDay)
      }));
    },

    onChangeMonth: function(newMonthText) {
      const newMonth = parseInt(newMonthText, 10);
      if (Number.isInteger(newMonth)) {
        this.props.onChange(this.props.recurrence.clone({
          month: newMonth,
          dayOfMonth: this.limitMaxDayForMonth(newMonth, this.getDay())
        }));
      } else {
        this.props.onChange(this.props.recurrence.clone({
          month: null
        }));
      }
    },

    render: function() {
      return (
        <div>
          <span className="align-button mrm">On the</span>
          <span className="mrm">
            <DayOfMonthInput value={this.getDay()} onChange={this.onChangeDay} />
          </span>
          <span className="align-button mrm">of</span>
          <div className="align-button height-xl">
            <Select className="form-select-s" value={this.getMonthText()} onChange={this.onChangeMonth}>
              <option value="" />
              {MONTH_NAMES.map((name, index) => (
                <option key={name} value={MONTH_ENUM_VALUES[index].toString()}>{name}</option>
              ))}
            </Select>
          </div>
        </div>
      );
    }
  });
});
