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

    typeMatches(typeName: RecurrenceType): boolean {
      return this.props.recurrence.typeName.indexOf(typeName) === 0;
    }

    set(newProps: Partial<RecurrenceInterface>): void {
      this.props.onChange(this.props.recurrence.clone(newProps));
    }

    setTypeMinutely(): void {
      this.props.onChange(this.props.recurrence.becomeMinutely());
    }

    setTypeHourly(): void {
      this.props.onChange(this.props.recurrence.becomeHourly());
    }

    setTypeDaily(): void {
      this.props.onChange(this.props.recurrence.becomeDaily({
        timeZone: this.props.teamTimeZone
      }));
    }

    setTypeWeekly(): void {
      this.props.onChange(this.props.recurrence.becomeWeekly({
        timeZone: this.props.teamTimeZone
      }));
    }

    setTypeMonthly(): void {
      this.props.onChange(this.props.recurrence.becomeMonthlyByDayOfMonth({
        timeZone: this.props.teamTimeZone
      }));
    }

    setTypeYearly(): void {
      this.props.onChange(this.props.recurrence.becomeYearly({
        timeZone: this.props.teamTimeZone
      }));
    }

    renderRecurrenceEditorForType() {
      if (this.typeMatches("yearly")) {
        return (
          <YearlyRecurrenceEditor recurrence={this.props.recurrence}
            onChange={this.props.onChange}
            teamTimeZone={this.props.teamTimeZone}
            teamTimeZoneName={this.props.teamTimeZoneName}
          />
        );
      } else if (this.typeMatches("monthly")) {
        return (
          <MonthlyRecurrenceEditor recurrence={this.props.recurrence}
            onChange={this.props.onChange}
            teamTimeZone={this.props.teamTimeZone}
            teamTimeZoneName={this.props.teamTimeZoneName}
          />
        );
      } else if (this.typeMatches("weekly")) {
        return (
          <WeeklyRecurrenceEditor recurrence={this.props.recurrence}
            onChange={this.props.onChange}
            teamTimeZone={this.props.teamTimeZone}
            teamTimeZoneName={this.props.teamTimeZoneName}
          />
        );
      } else if (this.typeMatches("daily")) {
        return (
          <DailyRecurrenceEditor recurrence={this.props.recurrence}
            onChange={this.props.onChange}
            teamTimeZone={this.props.teamTimeZone}
            teamTimeZoneName={this.props.teamTimeZoneName}
          />
        );
      } else if (this.typeMatches("hourly")) {
        return (
          <HourlyRecurrenceEditor recurrence={this.props.recurrence} onChange={this.props.onChange}/>
        );
      } else { /* this.typeMatches("minutely") or any future unknown type */
        return (
          <MinutelyRecurrenceEditor recurrence={this.props.recurrence} onChange={this.props.onChange}/>
        );
      }
    }

    render() {
      return (
        <div>
          <div className="mvm">
            <div className="align-button mrm type-s">Repeat</div>
            <div className="align-button">
              <ToggleGroup className="form-toggle-group-s">
                <ToggleGroup.Item
                  onClick={this.setTypeMinutely}
                  activeWhen={this.typeMatches("minutely")}
                  label="Minutely"
                />
                <ToggleGroup.Item
                  onClick={this.setTypeHourly}
                  activeWhen={this.typeMatches("hourly")}
                  label="Hourly"
                />
                <ToggleGroup.Item
                  onClick={this.setTypeDaily}
                  activeWhen={this.typeMatches("daily")}
                  label="Daily"
                />
                <ToggleGroup.Item
                  onClick={this.setTypeWeekly}
                  activeWhen={this.typeMatches("weekly")}
                  label="Weekly"
                />
                <ToggleGroup.Item
                  onClick={this.setTypeMonthly}
                  activeWhen={this.typeMatches("monthly")}
                  label="Monthly"
                />
                <ToggleGroup.Item
                  onClick={this.setTypeYearly}
                  activeWhen={this.typeMatches("yearly")}
                  label="Yearly"
                />
              </ToggleGroup>
            </div>
          </div>

          <div className="mvm">
            {this.renderRecurrenceEditorForType()}
          </div>
        </div>
      );
    }
}

export default RecurrenceEditor;
