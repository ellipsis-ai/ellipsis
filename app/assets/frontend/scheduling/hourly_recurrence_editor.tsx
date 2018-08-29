import * as React from 'react';
import FrequencyEditor from './frequency_editor';
import MinuteOfHourEditor from './minute_of_hour_editor';
import {RecurrenceEditorProps} from "./recurrence_editor";
import autobind from "../lib/autobind";

class HourlyRecurrenceEditor extends React.Component<RecurrenceEditorProps> {
    constructor(props: RecurrenceEditorProps) {
      super(props);
      autobind(this);
    }

    render() {
      return (
        <div>
          <div className="mvm">
            <MinuteOfHourEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
            />
          </div>
          <div className="mvm">
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
      );
    }
}

export default HourlyRecurrenceEditor;
