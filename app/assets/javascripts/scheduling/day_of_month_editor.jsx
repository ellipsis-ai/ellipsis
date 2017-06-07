define(function(require) {
  var React = require('react'),
    FormInput = require('../form/input'),
    Select = require('../form/select'),
    Recurrence = require('../models/recurrence');

  return React.createClass({
    displayName: 'DayOfMonthEditor',
    propTypes: {
      recurrence: React.PropTypes.instanceOf(Recurrence).isRequired,
      onChange: React.PropTypes.func.isRequired
    },

    isNthWeekdayOfMonth: function() {
      return typeof this.props.recurrence.nthDayOfWeek === "number" || typeof this.props.recurrence.dayOfWeek === "number";
    },

    getDay: function() {
      return [this.props.recurrence.dayOfMonth, this.props.recurrence.nthDayOfWeek].find((ea) => typeof(ea) === "number");
    },

    getDayOfWeekWithFallback: function() {
      return this.props.recurrence.dayOfWeek || 1;
    },

    getTextDay: function() {
      const day = this.getDay();
      if (typeof day === 'number') {
        return day.toString();
      } else {
        return "";
      }
    },

    getTextDayType: function() {
      if (this.isNthWeekdayOfMonth()) {
        return `weekday${this.getDayOfWeekWithFallback()}`;
      } else {
        return "dayOfMonth";
      }
    },

    onChangeDay: function(newValue) {
      if (this.isNthWeekdayOfMonth()) {
        this.onChangeNthWeekdayOfMonth(newValue);
      } else {
        this.onChangeDayOfMonth(newValue);
      }
    },

    onChangeDayOfMonth: function(newValue) {
      const parsed = newValue.substr(-2, 2).match(/(3[0-1]|[1-2][0-9]|[1-9])$/);
      let day;
      if (parsed) {
        day = parseInt(parsed, 10);
      }
      if (isNaN(day)) {
        day = null;
      }
      this.props.onChange(this.props.recurrence.clone({
        dayOfMonth: day,
        nthDayOfWeek: null,
        dayOfWeek: null
      }));
    },

    onChangeNthWeekdayOfMonth: function(newValue) {
      const parsed = newValue.substr(-1, 1).match(/^([1-5])$/);
      let day;
      if (parsed) {
        day = parseInt(parsed, 10);
      }
      if (isNaN(day)) {
        day = null;
      }
      this.props.onChange(this.props.recurrence.clone({
        dayOfMonth: null,
        nthDayOfWeek: day,
        dayOfWeek: this.getDayOfWeekWithFallback()
      }));
    },

    getOrdinalSuffix: function() {
      const lastDigit = this.getDay() % 10;
      const last2Digits = this.getDay() % 100;
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

    onChangeDayType: function(newValue) {
      if (newValue === "dayOfMonth") {
        this.props.onChange(this.props.recurrence.clone({
          dayOfMonth: this.getDay(),
          nthDayOfWeek: null,
          dayOfWeek: null
        }));
      } else {
        const parsed = newValue.match(/^weekday(\d)$/);
        let newDay;
        if (parsed) {
          newDay = parseInt(parsed, 10);
        }
        if (isNaN(newDay)) {
          newDay = 1;
        }
        this.props.onChange(this.props.recurrence.clone({
          dayOfMonth: null,
          nthDayOfWeek: Math.min(5, this.getDay()),
          dayOfWeek: newDay
        }));
      }
    },

    render: function() {
      return (
        <div>
          <span className="align-button mrm">On the</span>
          <FormInput
            className="width-2 form-input-borderless align-c"
            value={this.getTextDay()}
            onChange={this.onChangeDay}
          />
          <span className="align-button mrm type-label">{this.getOrdinalSuffix()}</span>
          <div className="align-button height-xl mrm">
            <Select className="form-select-s" value={this.getTextDayType()} onChange={this.onChangeDayType}>
              <option value="dayOfMonth">day</option>
              <option value="weekday1">Monday</option>
              <option value="weekday2">Tuesday</option>
              <option value="weekday3">Wednesday</option>
              <option value="weekday4">Thursday</option>
              <option value="weekday5">Friday</option>
              <option value="weekday6">Saturday</option>
              <option value="weekday0">Sunday</option>
            </Select>
          </div>
          <span className="align-button">of the month</span>
        </div>
      );
    }
  });
});
