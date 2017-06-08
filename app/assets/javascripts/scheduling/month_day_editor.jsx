define(function(require) {
  var React = require('react'),
    DayOfMonthInput = require('../form/day_of_month_input'),
    Select = require('../form/select'),
    Month = require('../models/month'),
    Recurrence = require('../models/recurrence');

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
      return new Month(this.props.recurrence.month).toString();
    },

    onChangeDay: function(newDay) {
      this.props.onChange(this.props.recurrence.clone({
        dayOfMonth: new Month(this.getMonth()).limitDaytoMax(newDay)
      }));
    },

    onChangeMonth: function(newMonthText) {
      const newMonth = Month.fromString(newMonthText);
      this.props.onChange(this.props.recurrence.clone({
        month: newMonth.value,
        dayOfMonth: newMonth.limitDaytoMax(this.getDay())
      }));
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
              {Month.YEAR.map((month) => (
                <option key={month.name()} value={month.toString()}>{month.name()}</option>
              ))}
            </Select>
          </div>
        </div>
      );
    }
  });
});
