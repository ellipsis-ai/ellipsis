import * as React from 'react';
import FrequencyEditor from './frequency_editor';
import MonthDayEditor from './month_day_editor';
import TimeOfDayEditor from './time_of_day_editor';
import {RecurrenceEditorProps, RecurrenceEditorTimeZoneProps} from "./recurrence_editor";
import autobind from "../lib/autobind";

type Props = RecurrenceEditorProps & RecurrenceEditorTimeZoneProps

class YearlyRecurrenceEditor extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  render() {
    return (
      <div>
        <div className="mvm pam border bg-white">
          <div className="mbm">
            <MonthDayEditor
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
          <FrequencyEditor
            recurrence={this.props.recurrence}
            onChange={this.props.onChange}
            unit="year"
            units="years"
            min={1}
            max={10}
          />
        </div>
      </div>
    );
  }
}

export default YearlyRecurrenceEditor;
