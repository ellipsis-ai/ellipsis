import * as React from 'react';
import DayOfMonthInput from '../form/day_of_month_input';
import Select from '../form/select';
import DayOfWeek from '../models/day_of_week';
import {RecurrenceEditorProps} from "./recurrence_editor";
import autobind from "../lib/autobind";

type Props = RecurrenceEditorProps;

class DayOfMonthEditor extends React.Component<Props> {
    constructor(props) {
      super(props);
      autobind(this);
    }

    isNthWeekdayOfMonth(): boolean {
      return (typeof this.props.recurrence.nthDayOfWeek === "number" && Number.isInteger(this.props.recurrence.nthDayOfWeek)) ||
        (typeof this.props.recurrence.dayOfWeek === "number" && Number.isInteger(this.props.recurrence.dayOfWeek));
    }

    getDay(): Option<number> {
      return [this.props.recurrence.dayOfMonth, this.props.recurrence.nthDayOfWeek].find((ea) => {
        return typeof ea === "number" && Number.isInteger(ea);
      });
    }

    getDayOfWeek(): Option<number> {
      return this.props.recurrence.dayOfWeek;
    }

    getTextDayType(): string {
      if (this.isNthWeekdayOfMonth()) {
        return new DayOfWeek(this.getDayOfWeek()).toString() || DayOfWeek.MONDAY.toString();
      } else {
        return "dayOfMonth";
      }
    }

    onChangeDay(newValue: Option<number>) {
      if (this.isNthWeekdayOfMonth()) {
        this.onChangeNthWeekdayOfMonth(newValue);
      } else {
        this.onChangeDayOfMonth(newValue);
      }
    }

    onChangeDayOfMonth(dayNumber: Option<number>) {
      this.props.onChange(this.props.recurrence.clone({
        typeName: "monthly_by_day_of_month",
        dayOfMonth: dayNumber,
        nthDayOfWeek: null,
        dayOfWeek: null
      }));
    }

    limitNthWeekdayNumber(dayNumber: Option<number>): number {
      if (typeof dayNumber === "number") {
        const lastDigit = dayNumber % 10;
        const limitMax = Math.min(lastDigit, 5);
        return Math.max(1, limitMax);
      } else {
        return 1;
      }
    }

    onChangeNthWeekdayOfMonth(dayNumber: Option<number>): void {
      const fixedDayNumber = typeof dayNumber === "number" && Number.isInteger(dayNumber) ? this.limitNthWeekdayNumber(dayNumber) : null;
      this.props.onChange(this.props.recurrence.clone({
        typeName: "monthly_by_nth_day_of_week",
        dayOfMonth: null,
        nthDayOfWeek: fixedDayNumber,
        dayOfWeek: this.getDayOfWeek()
      }));
    }

    onChangeDayType(newValue: string): void {
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
    }

    render() {
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
}

export default DayOfMonthEditor;
