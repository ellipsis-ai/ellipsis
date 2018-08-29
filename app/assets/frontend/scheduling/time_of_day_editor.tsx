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
      const hour = this.getHour();
      if (typeof hour === "number" && Number.isInteger(hour)) {
        this.lastValidHour = hour;
      }
    }

    componentDidMount(): void {
      this.updateLastValidHour();
    }

    componentDidUpdate(): void {
      this.updateLastValidHour();
    }

    isAM(): boolean {
      const hour = this.getHour() || this.lastValidHour;
      return Boolean(hour && Hour.isAM(hour));
    }

    setAM(): void {
      const hour = this.getHour();
      if (hour) {
        const newHour = Hour.convertToAM(hour);
        this.setHour(newHour);
      }
    }

    isPM(): boolean {
      const hour = this.getHour() || this.lastValidHour;
      return Boolean(hour && Hour.isPM(hour));
    }

    setPM(): void {
      const hour = this.getHour();
      if (hour) {
        const newHour = Hour.convertToPM(hour);
        this.setHour(newHour);
      }
    }

    getHour(): Option<number> {
      return this.props.recurrence.timeOfDay ? this.props.recurrence.timeOfDay.hour : null;
    }

    setHour(newHour: number): void {
      const minute = this.getMinute();
      if (typeof minute === "number") {
        this.props.onChange(this.props.recurrence.clone({
          timeOfDay: {
            hour: newHour,
            minute: minute
          }
        }));
      }
    }

    setMinute(newMinute: Option<number>): void {
      const hour = this.getHour();
      if (typeof hour === "number" && typeof newMinute === "number") {
        this.props.onChange(this.props.recurrence.clone({
          timeOfDay: {
            hour: hour,
            minute: newMinute
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
      const hour = Hour.fromString(newValue);
      let newHour;
      if (this.isAM() && hour) {
        newHour = hour.convertToAMValue();
      } else if (this.isPM()) {
        newHour = hour.convertToPMValue();
      } else {
        newHour = hour.value;
      }
      if (newHour) {
        this.setHour(newHour);
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
