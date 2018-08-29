import * as React from 'react';
import FrequencyEditor from './frequency_editor';
import autobind from "../lib/autobind";
import {RecurrenceEditorProps} from "./recurrence_editor";

class MinutelyRecurrenceEditor extends React.Component<RecurrenceEditorProps> {
  constructor(props: RecurrenceEditorProps) {
    super(props);
    autobind(this);
  }

  render() {
    return (
      <div>
        <FrequencyEditor
          recurrence={this.props.recurrence}
          onChange={this.props.onChange}
          unit="minute"
          units="minutes"
          min={1}
          max={525600}
        />
      </div>
    );
  }
}

export default MinutelyRecurrenceEditor;
