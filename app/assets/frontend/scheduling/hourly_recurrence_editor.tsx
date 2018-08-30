import * as React from 'react';
import FrequencyEditor from './frequency_editor';
import MinuteOfHourEditor from './minute_of_hour_editor';
import {RecurrenceEditorProps} from "./recurrence_editor";
import autobind from "../lib/autobind";
import RecurrenceTimesToRunEditor from "./recurrence_times_to_run_editor";

class HourlyRecurrenceEditor extends React.Component<RecurrenceEditorProps> {
  constructor(props: RecurrenceEditorProps) {
    super(props);
    autobind(this);
  }

  render() {
    return (
      <div>
        <div className="mvm pam border bg-white border-radius">
          <div>
            <MinuteOfHourEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
            />
          </div>
        </div>
        <div className="mvm pam border bg-white border-radius">
          <div className="mbm">
            <RecurrenceTimesToRunEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
            />
          </div>
          <div className="mtm">
            <FrequencyEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
              unit="hour"
              units="hours"
              min={1}
              max={8760}
            />
          </div>
        </div>
      </div>
    );
  }
}

export default HourlyRecurrenceEditor;
