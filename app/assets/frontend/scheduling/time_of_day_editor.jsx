import * as React from 'react';
import Collapsible from '../../javascripts/shared_ui/collapsible';
import FormInput from '../../javascripts/form/input';
import MinuteInput from '../form/minute_input';
import TimeZoneSelector from '../time_zone/time_zone_selector';
import Hour from '../models/hour';
import ToggleGroup from '../../javascripts/form/toggle_group';
import Recurrence from '../models/recurrence';

const TimeOfDayEditor = React.createClass({
    propTypes: {
      recurrence: React.PropTypes.instanceOf(Recurrence).isRequired,
      onChange: React.PropTypes.func.isRequired,
      teamTimeZone: React.PropTypes.string.isRequired,
      teamTimeZoneName: React.PropTypes.string.isRequired
    },

    getInitialState: function() {
      return {
        showTimeZones: false
      };
    },

    componentWillReceiveProps(nextProps) {
      if (this.props.recurrence.id !== nextProps.recurrence.id) {
        this.setState(this.getInitialState());
      }
    },

    lastValidHour: null,

    updateLastValidHour: function() {
      const hour = this.getHour();
      if (Number.isInteger(hour)) {
        this.lastValidHour = hour;
      }
    },

    componentDidMount: function() {
      this.updateLastValidHour();
    },

    componentDidUpdate: function() {
      this.updateLastValidHour();
    },

    isAM: function() {
      const hour = this.getHour();
      return Hour.isAM(Number.isInteger(hour) ? hour : this.lastValidHour);
    },

    setAM: function() {
      const newHour = Hour.convertToAM(this.getHour());
      this.setHour(newHour);
    },

    isPM: function() {
      const hour = this.getHour();
      return Hour.isPM(Number.isInteger(hour) ? hour : this.lastValidHour);
    },

    setPM: function() {
      const newHour = Hour.convertToPM(this.getHour());
      this.setHour(newHour);
    },

    getHour: function() {
      return this.props.recurrence.timeOfDay ? this.props.recurrence.timeOfDay.hour : null;
    },

    setHour: function(newHour) {
      this.props.onChange(this.props.recurrence.clone({
        timeOfDay: {
          hour: newHour,
          minute: this.getMinute()
        }
      }));
    },

    setMinute: function(newMinute) {
      this.props.onChange(this.props.recurrence.clone({
        timeOfDay: {
          hour: this.getHour(),
          minute: newMinute
        }
      }));
    },

    getMinute: function() {
      return this.props.recurrence.timeOfDay ? this.props.recurrence.timeOfDay.minute: null;
    },

    getHourTextValue: function() {
      const hour = this.getHour();
      return new Hour(hour).toString();
    },

    getCurrentTimeZoneName: function() {
      if (this.props.recurrence.timeZoneName) {
        return this.props.recurrence.timeZoneName;
      } else {
        return this.props.teamTimeZoneName;
      }
    },

    onChangeHour: function(newValue) {
      const hour = Hour.fromString(newValue);
      if (this.isAM()) {
        this.setHour(hour.convertToAMValue());
      } else if (this.isPM()) {
        this.setHour(hour.convertToPMValue());
      } else {
        this.setHour(hour.value);
      }
    },

    shouldShowTimeZones: function() {
      return this.state.showTimeZones;
    },

    showTimeZones: function() {
      this.setState({
        showTimeZones: true
      }, () => {
        if (this.timeZoneSelector) {
          this.timeZoneSelector.focus();
        }
      });
    },

    updateSelectedTimeZone: function(timeZoneId, cityName, timeZoneName) {
      this.props.onChange(this.props.recurrence.clone({
        timeZone: timeZoneId,
        timeZoneName: timeZoneName
      }));
    },

    render: function() {
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
});

export default TimeOfDayEditor;
