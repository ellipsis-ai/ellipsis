import * as React from 'react';
import DayOfMonthInput from '../form/day_of_month_input';
import Select from '../form/select';
import Month from '../models/month';
import Recurrence from '../models/recurrence';

const MonthDayEditor = React.createClass({
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
          <span className="align-button mrm type-s">On the</span>
          <span className="mrm">
            <DayOfMonthInput value={this.getDay()} onChange={this.onChangeDay} />
          </span>
          <span className="align-button mrm type-s">of</span>
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

export default MonthDayEditor;
