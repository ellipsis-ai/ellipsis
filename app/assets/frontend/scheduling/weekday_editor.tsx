import * as React from 'react';
import Checkbox from '../form/checkbox';
import DayOfWeek from '../models/day_of_week';
import {RecurrenceEditorProps} from "./recurrence_editor";
import autobind from "../lib/autobind";

type Props = RecurrenceEditorProps

class WeekdayEditor extends React.Component<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    getWeekdays(): Array<number> {
      return this.props.recurrence.daysOfWeek;
    }

    onChange(newWeekdays: Array<number>) {
      this.props.onChange(this.props.recurrence.clone({
        daysOfWeek: newWeekdays
      }));
    }

    weekdaysInclude(day: Option<number>): boolean {
      return Boolean(typeof day === "number" && this.getWeekdays().includes(day));
    }

    onChangeDay(isChecked: boolean, stringValue?: Option<string>): void {
      if (typeof stringValue === "string") {
        const day = DayOfWeek.fromString(stringValue).value;
        let weekdays: Array<number>;
        if (isChecked && typeof day === "number" && !this.weekdaysInclude(day)) {
          weekdays = this.getWeekdays().concat(day);
        } else {
          weekdays = this.getWeekdays().filter((ea) => ea !== day);
        }
        const timesToRun = this.props.recurrence.totalTimesToRun;
        if (timesToRun && weekdays.length > timesToRun) {
          weekdays = weekdays.slice(weekdays.length - timesToRun);
        }
        this.onChange(weekdays);
      }
    }

    render() {
      return (
        <div>
          <span className="align-button mrm type-s">On</span>
          <span className="align-button">
            {DayOfWeek.WEEK.map((day) => (
              <Checkbox key={day.name()}
                className="mrm"
                checked={this.weekdaysInclude(day.value)}
                label={day.shortName()}
                title={day.name()}
                value={day.toString()}
                onChange={this.onChangeDay}
                useButtonStyle={true}
              />
            ))}
          </span>
        </div>
      );
    }
}

export default WeekdayEditor;
