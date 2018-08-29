import * as React from 'react';
import {RecurrenceEditorProps, RecurrenceEditorTimeZoneProps} from "./recurrence_editor";
import autobind from "../lib/autobind";
import ToggleGroup, {ToggleGroupItem} from "../form/toggle_group";
import FormInput from "../form/input";
import OptionalInt from "../models/optional_int";

type Props = RecurrenceEditorProps & RecurrenceEditorTimeZoneProps

class RecurrenceRepeatEditor extends React.Component<Props> {
  totalTimesInput: Option<FormInput>;

  constructor(props) {
    super(props);
    autobind(this);
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

  toggleTimesToRun(enableTimesToRun: boolean): void {
    const newTotal = enableTimesToRun ? (this.props.recurrence.totalTimesToRun || 1) : null;
    this.setTotalTimesToRun(newTotal);
  }

  setIndefiniteRepeat() {
    this.toggleTimesToRun(false);
  }

  setLimitedRepeat() {
    this.toggleTimesToRun(true);
    if (this.totalTimesInput) {
      this.totalTimesInput.focus();
    }
  }

  setTotalTimesFromString(newStringValue: string) {
    const newValue = OptionalInt.fromString(newStringValue).valueWithinRange(1, 99999999);
    this.setTotalTimesToRun(newValue);
  }

  setTotalTimesToRun(timesToRun: Option<number>): void {
    const totalHasChanged = timesToRun !== this.props.recurrence.totalTimesToRun;
    const frequency = timesToRun === 1 ? 1 : this.props.recurrence.frequency;
    let daysOfWeek = this.props.recurrence.daysOfWeek;
    if (timesToRun && timesToRun < daysOfWeek.length) {
      daysOfWeek = daysOfWeek.slice(daysOfWeek.length - timesToRun);
    }
    this.props.onChange(this.props.recurrence.clone({
      frequency: frequency,
      daysOfWeek: daysOfWeek,
      totalTimesToRun: timesToRun,
      timesHasRun: totalHasChanged ? 0 : this.props.recurrence.timesHasRun
    }));
  }

  getTotalTimesString(): string {
    return String(this.props.recurrence.totalTimesToRun || "");
  }

  getTotalTimesUnit(): string {
    if (this.props.recurrence.totalTimesToRun === 1) {
      return "time";
    } else {
      return "times";
    }
  }

  getTimesRemaining(): string {
    const totalTimes = this.props.recurrence.totalTimesToRun;
    const timesRemaining = totalTimes ?
      totalTimes - this.props.recurrence.timesHasRun : null;
    if (timesRemaining && totalTimes && timesRemaining < totalTimes) {
      return timesRemaining === 1 ?
        "(1 more time remaining)" : `(${timesRemaining} more times remaining)`;
    } else {
      return "";
    }
  }

  renderRepeatLimit() {
    if (this.props.recurrence.typeName !== "yearly") {
      return (
        <div className="mvm">
          <div className="align-button mrm type-s">
            How many times
          </div>
          <div className="align-button mrm">
            <ToggleGroup className="form-toggle-group-s">
              <ToggleGroupItem
                activeWhen={!this.props.recurrence.totalTimesToRun}
                label={"Forever"}
                onClick={this.setIndefiniteRepeat}
              />
              <ToggleGroupItem
                activeWhen={Boolean(this.props.recurrence.totalTimesToRun)}
                label={"Limit to:"}
                onClick={this.setLimitedRepeat}
              />
            </ToggleGroup>
          </div>
          <div className="display-inline-block mrm">
            <FormInput
              ref={(el) => this.totalTimesInput = el}
              className="width-5 form-input-borderless align-c"
              onChange={this.setTotalTimesFromString}
              value={this.getTotalTimesString()}
              placeholder={"1 or more"}
            />
          </div>
          <div className="align-button type-s mrm">
            {this.getTotalTimesUnit()}
          </div>
          <div className="align-button type-disabled type-s">
            {this.getTimesRemaining()}
          </div>
        </div>
      );
    } else {
      return null;
    }
  }

  render() {
    return (
      <div>
        <div className="mvm">
          <div className="align-button type-s mrm">Interval</div>
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
        {this.renderRepeatLimit()}
      </div>
    )
  }
}

export default RecurrenceRepeatEditor;
