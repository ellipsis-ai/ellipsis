import * as React from 'react';
import ToggleGroup from '../form/toggle_group';
import Recurrence, {RecurrenceInterface, RecurrenceType} from '../models/recurrence';
import MinutelyRecurrenceEditor from './minutely_recurrence_editor';
import HourlyRecurrenceEditor from './hourly_recurrence_editor';
import DailyRecurrenceEditor from './daily_recurrence_editor';
import WeeklyRecurrenceEditor from './weekly_recurrence_editor';
import MonthlyRecurrenceEditor from './monthly_recurrence_editor';
import YearlyRecurrenceEditor from './yearly_recurrence_editor';
import autobind from "../lib/autobind";
import RecurrenceRepeatEditor from "./recurrence_repeat_editor";

export interface RecurrenceEditorProps {
  recurrence: Recurrence,
  onChange: (recurrence: Recurrence) => void,
}

export interface RecurrenceEditorTimeZoneProps {
  teamTimeZone: string,
  teamTimeZoneName: string
}

type Props = RecurrenceEditorProps & RecurrenceEditorTimeZoneProps

class RecurrenceEditor extends React.Component<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    renderRecurrenceEditorForType() {
      if (this.props.recurrence.typeName === "yearly") {
        return (
          <YearlyRecurrenceEditor recurrence={this.props.recurrence}
            onChange={this.props.onChange}
            teamTimeZone={this.props.teamTimeZone}
            teamTimeZoneName={this.props.teamTimeZoneName}
          />
        );
      } else if (this.props.recurrence.typeName.indexOf("monthly") === 0) {
        return (
          <MonthlyRecurrenceEditor recurrence={this.props.recurrence}
            onChange={this.props.onChange}
            teamTimeZone={this.props.teamTimeZone}
            teamTimeZoneName={this.props.teamTimeZoneName}
          />
        );
      } else if (this.props.recurrence.typeName === "weekly") {
        return (
          <WeeklyRecurrenceEditor recurrence={this.props.recurrence}
            onChange={this.props.onChange}
            teamTimeZone={this.props.teamTimeZone}
            teamTimeZoneName={this.props.teamTimeZoneName}
          />
        );
      } else if (this.props.recurrence.typeName === "daily") {
        return (
          <DailyRecurrenceEditor recurrence={this.props.recurrence}
            onChange={this.props.onChange}
            teamTimeZone={this.props.teamTimeZone}
            teamTimeZoneName={this.props.teamTimeZoneName}
          />
        );
      } else if (this.props.recurrence.typeName === "hourly") {
        return (
          <HourlyRecurrenceEditor recurrence={this.props.recurrence} onChange={this.props.onChange}/>
        );
      } else { /* this.props.recurrence.typeName === "minutely" or any future unknown type */
        return (
          <MinutelyRecurrenceEditor recurrence={this.props.recurrence} onChange={this.props.onChange}/>
        );
      }
    }

    render() {
      return (
        <div>
          <div className="mvm">
            <RecurrenceRepeatEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
              teamTimeZone={this.props.teamTimeZone}
              teamTimeZoneName={this.props.teamTimeZoneName}
            />
          </div>

          <div className="mvm">
            {this.renderRecurrenceEditorForType()}
          </div>

        </div>
      );
    }
}

export default RecurrenceEditor;
