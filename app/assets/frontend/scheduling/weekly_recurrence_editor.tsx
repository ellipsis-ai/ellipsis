import * as React from 'react';
import FrequencyEditor from './frequency_editor';
import TimeOfDayEditor from './time_of_day_editor';
import WeekdayEditor from './weekday_editor';
import {RecurrenceEditorProps, RecurrenceEditorTimeZoneProps} from "./recurrence_editor";
import autobind from "../lib/autobind";
import RecurrenceTimesToRunEditor from "./recurrence_times_to_run_editor";

type Props = RecurrenceEditorProps & RecurrenceEditorTimeZoneProps

class WeeklyRecurrenceEditor extends React.Component<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    render() {
      return (
        <div>
          <div className="mvm">
            <WeekdayEditor
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
          <div className="mvm">
            <RecurrenceTimesToRunEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
            />
          </div>
          {this.props.recurrence.totalTimesToRun === 1 ? null : (
            <div className="mvm">
              <FrequencyEditor
                recurrence={this.props.recurrence}
                onChange={this.props.onChange}
                unit="week"
                units="weeks"
                min={1}
                max={520}
              />
            </div>
          )}
        </div>
      );
    }
}

export default WeeklyRecurrenceEditor;
