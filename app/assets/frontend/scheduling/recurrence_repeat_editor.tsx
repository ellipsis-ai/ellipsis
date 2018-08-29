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
      timeZone: this.props.teamTimeZone
    }));
  }

  toggleTimesToRun(enableTimesToRun: boolean): void {
    this.props.onChange(this.props.recurrence.clone({
      totalTimesToRun: enableTimesToRun ? 1 : null
    }));
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

  setTotalTimes(newStringValue: string) {
    this.props.onChange(this.props.recurrence.clone({
      totalTimesToRun: OptionalInt.fromString(newStringValue).valueWithinRange(1, 99999999)
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

  render() {
    return (
      <div>
        <div className="mvm">
          <div className="align-button type-s mrm">Interval</div>
          <div className="align-button mrm">
            <ToggleGroup className="form-toggle-group-s">
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
                activeWhen={this.props.recurrence.typeName.indexOf("monthly_by_day_of_month") === 0}
                label="Monthly"
              />
              <ToggleGroupItem
                onClick={this.setTypeYearly}
                activeWhen={this.props.recurrence.typeName === "yearly"}
                label="Yearly"
              />
            </ToggleGroup>
          </div>
        </div>
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
              onChange={this.setTotalTimes}
              value={this.getTotalTimesString()}
              placeholder={"1 or more"}
            />
          </div>
          <div className="align-button">
            {this.getTotalTimesUnit()}
          </div>

        </div>
      </div>
    )
  }
}

export default RecurrenceRepeatEditor;
