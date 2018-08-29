import * as React from 'react';
import DayOfMonthEditor from './day_of_month_editor';
import FrequencyEditor from './frequency_editor';
import TimeOfDayEditor from './time_of_day_editor';
import {RecurrenceEditorProps, RecurrenceEditorTimeZoneProps} from "./recurrence_editor";
import autobind from "../lib/autobind";

type Props = RecurrenceEditorProps & RecurrenceEditorTimeZoneProps

class MonthlyRecurrenceEditor extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  render() {
      return (
        <div>
          <div className="mvm">
            <FrequencyEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
              unit="month"
              units="months"
              min={1}
              max={120}
            />
          </div>
          <div className="mvm">
            <DayOfMonthEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
            />
          </div>
          <div className="mvm">
            <TimeOfDayEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
              teamTimeZone={this.props.teamTimeZone}
              teamTimeZoneName={this.props.teamTimeZoneName}
            />
          </div>
        </div>
      );
    }
}

export default MonthlyRecurrenceEditor;