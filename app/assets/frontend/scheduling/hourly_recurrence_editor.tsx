import * as React from 'react';
import FrequencyEditor from './frequency_editor';
import MinuteOfHourEditor from './minute_of_hour_editor';
import {RecurrenceEditorProps, RecurrenceEditorTimeZoneProps} from "./recurrence_editor";
import autobind from "../lib/autobind";
import RecurrenceTimesToRunEditor from "./recurrence_times_to_run_editor";
import TimeZoneSelector from "../time_zone/time_zone_selector";
import Collapsible from "../shared_ui/collapsible";

interface State {
  showTimeZones: boolean
}

type Props = RecurrenceEditorProps & RecurrenceEditorTimeZoneProps

class HourlyRecurrenceEditor extends React.Component<Props, State> {
  timeZoneSelector: Option<TimeZoneSelector>;

  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = this.defaultState();
  }

  defaultState(): State {
    return {
      showTimeZones: false
    };
  }

  componentWillReceiveProps(nextProps: Props): void {
    if (this.props.recurrence.id !== nextProps.recurrence.id) {
      this.setState(this.defaultState());
    }
  }

  getCurrentTimeZoneName(): string {
    if (this.props.recurrence.timeZoneName) {
      return this.props.recurrence.timeZoneName;
    } else {
      return this.props.teamTimeZoneName;
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

  updateSelectedTimeZone(timeZoneId: string, cityName: string, timeZoneName: string): void {
    this.props.onChange(this.props.recurrence.clone({
      timeZone: timeZoneId,
      timeZoneName: timeZoneName
    }));
  }

  render() {
    return (
      <div>
        <div className="mvm pam border bg-white">
          <div>
            <span className="display-inline-block mrm">
              <MinuteOfHourEditor
                recurrence={this.props.recurrence}
                onChange={this.props.onChange}
              />
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
        <div className="mvm pam border bg-white">
          <div className="mbm">
            <RecurrenceTimesToRunEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
            />
          </div>
          <div className="mtm">
            <FrequencyEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
              unit="hour"
              units="hours"
              min={1}
              max={8760}
            />
          </div>
        </div>
      </div>
    );
  }
}

export default HourlyRecurrenceEditor;
