import * as React from 'react';
import DayOfMonthEditor from './day_of_month_editor';
import FrequencyEditor from './frequency_editor';
import TimeOfDayEditor from './time_of_day_editor';
import {RecurrenceEditorProps, RecurrenceEditorTimeZoneProps} from "./recurrence_editor";
import autobind from "../lib/autobind";
import RecurrenceTimesToRunEditor from "./recurrence_times_to_run_editor";

type Props = RecurrenceEditorProps & RecurrenceEditorTimeZoneProps

class MonthlyRecurrenceEditor extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  render() {
    return (
      <div>
        <div className="mvm pam border bg-white">
          <div className="mbm">
            <DayOfMonthEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
            />
          </div>
          <div className="mtm">
            <TimeOfDayEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
              teamTimeZone={this.props.teamTimeZone}
              teamTimeZoneName={this.props.teamTimeZoneName}
            />
          </div>
        </div>
        <div className="mvm pam border bg-white">
          <div>
            <RecurrenceTimesToRunEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
            />
          </div>
          {this.props.recurrence.totalTimesToRun === 1 ? null : (
            <div className="mtm">
              <FrequencyEditor
                recurrence={this.props.recurrence}
                onChange={this.props.onChange}
                unit="month"
                units="months"
                min={1}
                max={120}
              />
            </div>
          )}
        </div>
      </div>
    );
  }
}

export default MonthlyRecurrenceEditor;
