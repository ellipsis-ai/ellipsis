define(function(require) {
  var React = require('react'),
    DayOfMonthInput = require('../form/day_of_month_input'),
    Select = require('../form/select'),
    DayOfWeek = require('../models/day_of_week'),
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

    getDayOfWeek: function() {
      return this.props.recurrence.dayOfWeek;
    },

    getTextDayType: function() {
      if (this.isNthWeekdayOfMonth()) {
        return new DayOfWeek(this.getDayOfWeek()).toString() || DayOfWeek.MONDAY.toString();
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
        dayOfWeek: this.getDayOfWeek()
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
        this.props.onChange(this.props.recurrence.clone({
          typeName: "monthly_by_nth_day_of_week",
          dayOfMonth: null,
          nthDayOfWeek: this.limitNthWeekdayNumber(this.getDay()),
          dayOfWeek: DayOfWeek.fromString(newValue).value
        }));
      }
    },

    render: function() {
      return (
        <div>
          <span className="align-button mrm type-s">On the</span>
          <span className="mrm">
            <DayOfMonthInput value={this.getDay()} onChange={this.onChangeDay} />
          </span>
          <div className="align-button height-xl mrm">
            <Select className="form-select-s" value={this.getTextDayType()} onChange={this.onChangeDayType}>
              <option value="dayOfMonth">day</option>
              {DayOfWeek.WEEK.map((day) => (
                <option key={day.name()} value={day.toString()}>{day.name()}</option>
              ))}
            </Select>
          </div>
          <span className="align-button type-s">of the month</span>
        </div>
      );
    }
  });
});
