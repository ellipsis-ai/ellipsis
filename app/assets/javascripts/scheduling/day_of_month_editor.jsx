define(function(require) {
  var React = require('react'),
    DayOfMonthInput = require('../form/day_of_month_input'),
    Select = require('../form/select'),
    OptionalInt = require('../models/optional_int'),
    Recurrence = require('../models/recurrence');

  return React.createClass({
    displayName: 'DayOfMonthEditor',
    propTypes: {
      recurrence: React.PropTypes.instanceOf(Recurrence).isRequired,
      onChange: React.PropTypes.func.isRequired
    },

    isNthWeekdayOfMonth: function() {
      return Number.isInteger(this.props.recurrence.nthDayOfWeek) || Number.isInteger(this.props.recurrence.dayOfWeek);
    },

    getDay: function() {
      return [this.props.recurrence.dayOfMonth, this.props.recurrence.nthDayOfWeek].find(Number.isInteger);
    },

    getDayOfWeekWithFallback: function() {
      return this.props.recurrence.dayOfWeek || 1;
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

    onChangeDayOfMonth: function(dayNumber) {
      this.props.onChange(this.props.recurrence.clone({
        typeName: "monthly_by_day_of_month",
        dayOfMonth: dayNumber,
        nthDayOfWeek: null,
        dayOfWeek: null
      }));
    },

    limitNthWeekdayNumber: function(dayNumber) {
      const lastDigit = dayNumber % 10;
      const limitMax = Math.min(lastDigit, 5);
      return Math.max(1, limitMax);
    },

    onChangeNthWeekdayOfMonth: function(dayNumber) {
      const fixedDayNumber = Number.isInteger(dayNumber) ? this.limitNthWeekdayNumber(dayNumber) : null;
      this.props.onChange(this.props.recurrence.clone({
        typeName: "monthly_by_nth_day_of_week",
        dayOfMonth: null,
        nthDayOfWeek: fixedDayNumber,
        dayOfWeek: this.getDayOfWeekWithFallback()
      }));
    },

    onChangeDayType: function(newValue) {
      if (newValue === "dayOfMonth") {
        this.props.onChange(this.props.recurrence.clone({
          typeName: "monthly_by_day_of_month",
          dayOfMonth: this.getDay(),
          nthDayOfWeek: null,
          dayOfWeek: null
        }));
      } else {
        const parsed = newValue.match(/^weekday(\d)$/);
        const newDay = OptionalInt.fromStringWithDefault(parsed ? parsed[1] : "", 1);
        this.props.onChange(this.props.recurrence.clone({
          typeName: "monthly_by_nth_day_of_week",
          dayOfMonth: null,
          nthDayOfWeek: this.limitNthWeekdayNumber(this.getDay()),
          dayOfWeek: newDay.value
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
