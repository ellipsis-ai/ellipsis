import * as React from 'react';
import DayOfMonthInput from '../form/day_of_month_input';
import Select from '../form/select';
import Month from '../models/month';
import {RecurrenceEditorProps} from "./recurrence_editor";
import autobind from "../lib/autobind";

type Props = RecurrenceEditorProps

class MonthDayEditor extends React.Component<Props> {
    constructor(props) {
      super(props);
      autobind(this);
    }

    getPrefix(): string {
      if (this.props.recurrence.typeName === "yearly" && this.props.recurrence.totalTimesToRun === 1) {
        return "On the";
      } else {
        return "Starting on the";
      }
    }

    getDay(): Option<number> {
      return this.props.recurrence.dayOfMonth;
    }

    getMonth(): Option<number> {
      return this.props.recurrence.month;
    }

    getMonthText(): string {
      return new Month(this.props.recurrence.month).toString();
    }

    onChangeDay(newDay: Option<number>) {
      this.props.onChange(this.props.recurrence.clone({
        dayOfMonth: newDay ? new Month(this.getMonth()).limitDaytoMax(newDay) : null
      }));
    }

    onChangeMonth(newMonthText: string) {
      const newMonth = Month.fromString(newMonthText);
      this.props.onChange(this.props.recurrence.clone({
        month: newMonth.value,
        dayOfMonth: newMonth.limitDaytoMax(this.getDay())
      }));
    }

    render() {
      return (
        <div>
          <span className="align-button mrm type-s">{this.getPrefix()}</span>
          <span className="mrm">
            <DayOfMonthInput value={this.getDay()} onChange={this.onChangeDay} />
          </span>
          <span className="align-button mrm type-s">of</span>
          <div className="align-button mrm height-xl">
            <Select className="form-select-s" value={this.getMonthText()} onChange={this.onChangeMonth}>
              {Month.YEAR.map((month) => (
                <option key={month.name()} value={month.toString()}>{month.name()}</option>
              ))}
            </Select>
          </div>
        </div>
      );
    }
}

export default MonthDayEditor;
