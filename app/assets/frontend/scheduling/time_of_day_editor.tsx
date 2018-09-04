import * as React from 'react';
import Collapsible from '../shared_ui/collapsible';
import FormInput from '../form/input';
import MinuteInput from '../form/minute_input';
import TimeZoneSelector from '../time_zone/time_zone_selector';
import Hour from '../models/hour';
import ToggleGroup from '../form/toggle_group';
import {RecurrenceEditorProps, RecurrenceEditorTimeZoneProps} from "./recurrence_editor";
import autobind from "../lib/autobind";

type Props = RecurrenceEditorProps & RecurrenceEditorTimeZoneProps

interface State {
  showTimeZones: boolean
}

class TimeOfDayEditor extends React.Component<Props, State> {
    lastValidHour: Option<number>;
    timeZoneSelector: Option<TimeZoneSelector>;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.state = this.defaultState();
      this.lastValidHour = null;
    }

    defaultState(): State {
      return {
        showTimeZones: false
      };
    }

    componentWillReceiveProps(nextProps: Props) {
      if (this.props.recurrence.id !== nextProps.recurrence.id) {
        this.setState(this.defaultState());
      }
    }

    updateLastValidHour(): void {
      const hourValue = this.getHour();
      if (typeof hourValue === "number" && Number.isInteger(hourValue)) {
        this.lastValidHour = hourValue;
      }
    }

    componentDidMount(): void {
      this.updateLastValidHour();
    }

    componentDidUpdate(): void {
      this.updateLastValidHour();
    }

    isAM(): boolean {
      const hourValue = this.getHour() || this.lastValidHour;
      return Boolean(typeof hourValue === "number" && Hour.isAM(hourValue));
    }

    setAM(): void {
      const hourValue = this.getHour();
      if (typeof hourValue === "number") {
        const newHourValue = Hour.convertToAM(hourValue);
        this.setHour(newHourValue);
      }
    }

    isPM(): boolean {
      const hourValue = this.getHour() || this.lastValidHour;
      return Boolean(typeof hourValue === "number" && Hour.isPM(hourValue));
    }

    setPM(): void {
      const hourValue = this.getHour();
      if (typeof hourValue === "number") {
        const newHourValue = Hour.convertToPM(hourValue);
        this.setHour(newHourValue);
      }
    }

    getHour(): Option<number> {
      return this.props.recurrence.timeOfDay ? this.props.recurrence.timeOfDay.hour : null;
    }

    setHour(newHourValue: number): void {
      const minuteValue = this.getMinute();
      if (typeof minuteValue === "number") {
        this.props.onChange(this.props.recurrence.clone({
          timeOfDay: {
            hour: newHourValue,
            minute: minuteValue
          }
        }));
      }
    }

    setMinute(newMinuteValue: Option<number>): void {
      const hourValue = this.getHour();
      if (typeof hourValue === "number" && typeof newMinuteValue === "number") {
        this.props.onChange(this.props.recurrence.clone({
          timeOfDay: {
            hour: hourValue,
            minute: newMinuteValue
          }
        }));
      }
    }

    getMinute(): Option<number> {
      return this.props.recurrence.timeOfDay ? this.props.recurrence.timeOfDay.minute: null;
    }

    getHourTextValue(): string {
      const hour = this.getHour();
      return new Hour(hour).toString();
    }

    getCurrentTimeZoneName(): string {
      if (this.props.recurrence.timeZoneName) {
        return this.props.recurrence.timeZoneName;
      } else {
        return this.props.teamTimeZoneName;
      }
    }

    onChangeHour(newValue: string): void {
      const hourObj = Hour.fromString(newValue);
      let newHourValue;
      if (this.isPM()) {
        newHourValue = hourObj.convertToPMValue();
      } else {
        newHourValue = hourObj.convertToAMValue();
      }
      if (typeof newHourValue === "number") {
        this.setHour(newHourValue);
      }
    }

    shouldShowTimeZones(): boolean {
      return this.state.showTimeZones;
    }

    showTimeZones(): void {
      this.setState({
        showTimeZones: true
      }, () => {
        if (this.timeZoneSelector) {
          this.timeZoneSelector.focus();
        }
      });
    }

    updateSelectedTimeZone(timeZoneId: string, cityName: string, timeZoneName: string) {
      this.props.onChange(this.props.recurrence.clone({
        timeZone: timeZoneId,
        timeZoneName: timeZoneName
      }));
    }

    render() {
      return (
        <div>
          <div>
            <span className="align-button mrm type-s">At</span>
            <FormInput
              className="width-2 form-input-borderless align-c"
              value={this.getHourTextValue()}
              onChange={this.onChangeHour}
            />
            <MinuteInput value={this.getMinute()} onChange={this.setMinute} />
            <span className="align-button mhm">
              <ToggleGroup className="form-toggle-group-s">
                <ToggleGroup.Item onClick={this.setAM} label="AM" activeWhen={this.isAM()} />
                <ToggleGroup.Item onClick={this.setPM} label="PM" activeWhen={this.isPM()} />
              </ToggleGroup>
            </span>
            <span className="align-button type-s">
              {this.shouldShowTimeZones() ? (
                <span>{this.getCurrentTimeZoneName()}</span>
              ) : (
                <button type="button" className="button-raw" onClick={this.showTimeZones}>
                  <span className="type-black">{this.getCurrentTimeZoneName()}</span>
                  <span> — Modify</span>
                </button>
              )}
            </span>
          </div>
          <Collapsible revealWhen={this.shouldShowTimeZones()}>
            {this.shouldShowTimeZones() ? (
              <TimeZoneSelector
                ref={(selector) => this.timeZoneSelector = selector}
                defaultTimeZone={this.props.recurrence.timeZone}
                onChange={this.updateSelectedTimeZone}
              />
            ) : (
              <div/>
            )}
          </Collapsible>
        </div>
      );
    }
}

export default TimeOfDayEditor;
