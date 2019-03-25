import * as React from 'react';
import MinuteInput from '../form/minute_input';
import {RecurrenceEditorProps} from "./recurrence_editor";
import autobind from "../lib/autobind";

class MinuteOfHourEditor extends React.Component<RecurrenceEditorProps> {
    constructor(props: RecurrenceEditorProps) {
      super(props);
      autobind(this);
    }

    getValue(): Option<number> {
      return this.props.recurrence.minuteOfHour;
    }

    onChange(newValue: Option<number>) {
      if (typeof newValue === "number") {
        this.props.onChange(this.props.recurrence.clone({
          minuteOfHour: newValue
        }));
      }
    }

    getSuffix(): string {
      const value = this.getValue();
      if (!value) {
        return "(on the hour)";
      } else if (value === 1) {
        return "minute past the hour";
      } else {
        return "minutes past the hour";
      }
    }

    render() {
      return (
        <div>
          <span className="align-button mrm type-s">At</span>
          <MinuteInput value={this.getValue()} onChange={this.onChange} />
          <span className="align-button mlm type-s">{this.getSuffix()}</span>
        </div>
      );
    }
}

export default MinuteOfHourEditor;
