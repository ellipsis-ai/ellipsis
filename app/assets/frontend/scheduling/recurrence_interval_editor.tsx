import * as React from 'react';
import {RecurrenceEditorProps, RecurrenceEditorTimeZoneProps} from "./recurrence_editor";
import autobind from "../lib/autobind";
import ToggleGroup, {ToggleGroupItem} from "../form/toggle_group";

type Props = RecurrenceEditorProps & RecurrenceEditorTimeZoneProps

class RecurrenceIntervalEditor extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  setTypeMinutely(): void {
    this.props.onChange(this.props.recurrence.becomeMinutely());
  }

  setTypeHourly(): void {
    this.props.onChange(this.props.recurrence.becomeHourly({
      timeZone: this.props.teamTimeZone
    }));
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
      timeZone: this.props.teamTimeZone,
      totalTimesToRun: null
    }));
  }

  setTypeOnce(): void {
    this.props.onChange(this.props.recurrence.becomeYearly({
      frequency: 1,
      timeZone: this.props.teamTimeZone,
      totalTimesToRun: 1
    }))
  }

  render() {
    return (
      <div>
        <div className="mvm">
          <div className="align-button mrm">
            <ToggleGroup className="form-toggle-group-s">
              <ToggleGroupItem
                activeWhen={this.props.recurrence.typeName === "yearly" && this.props.recurrence.totalTimesToRun === 1}
                label={"Single date/time"}
                onClick={this.setTypeOnce}
              />
              <ToggleGroupItem
                onClick={this.setTypeMinutely}
                activeWhen={this.props.recurrence.typeName === "minutely"}
                label="Minutely"
              />
              <ToggleGroupItem
                onClick={this.setTypeHourly}
                activeWhen={this.props.recurrence.typeName === "hourly"}
                label="Hourly"
              />
              <ToggleGroupItem
                onClick={this.setTypeDaily}
                activeWhen={this.props.recurrence.typeName === "daily"}
                label="Daily"
              />
              <ToggleGroupItem
                onClick={this.setTypeWeekly}
                activeWhen={this.props.recurrence.typeName === "weekly"}
                label="Weekly"
              />
              <ToggleGroupItem
                onClick={this.setTypeMonthly}
                activeWhen={this.props.recurrence.typeName.indexOf("monthly") === 0}
                label="Monthly"
              />
              <ToggleGroupItem
                onClick={this.setTypeYearly}
                activeWhen={this.props.recurrence.typeName === "yearly" && this.props.recurrence.totalTimesToRun !== 1}
                label="Yearly"
              />
            </ToggleGroup>
          </div>
        </div>
      </div>
    )
  }
}

export default RecurrenceIntervalEditor;
