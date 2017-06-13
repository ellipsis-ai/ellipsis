define(function(require) {
  var React = require('react'),
    Collapsible = require('../shared_ui/collapsible'),
    FormInput = require('../form/input'),
    MinuteInput = require('../form/minute_input'),
    TimeZoneSetter = require('../time_zone/time_zone_selector'),
    Hour = require('../models/hour'),
    ToggleGroup = require('../form/toggle_group'),
    Recurrence = require('../models/recurrence');

  return React.createClass({
    displayName: 'TimeOfDayEditor',
    propTypes: {
      recurrence: React.PropTypes.instanceOf(Recurrence).isRequired,
      onChange: React.PropTypes.func.isRequired,
      teamTimeZone: React.PropTypes.string.isRequired
    },

    getInitialState: function() {
      return {
        showTimeZones: false,
        selectedTimeZoneId: "",
        selectedTimeZoneName: "",
        currentTimeZoneName: ""
      };
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
      if (this.state.currentTimeZoneName) {
        return this.state.currentTimeZoneName;
      } else {
        const timeZone = this.props.recurrence.timeZone || this.props.teamTimeZone;
        const humanized = timeZone.replace(/^.+\//, "").replace(/_/g, " ");
        return `${humanized} time`;
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
      });
    },

    setTimeZone: function() {
      this.setState({
        currentTimeZoneName: this.state.selectedTimeZoneName,
        showTimeZones: false
      }, () => {
        this.props.onChange(this.props.recurrence.clone({
          timeZone: this.state.selectedTimeZoneId
        }));
      });
    },

    cancelSetTimeZone: function() {
      this.setState({
        showTimeZones: false
      });
    },

    recurrenceTimeZoneMatches(timeZoneId) {
      return timeZoneId === this.props.recurrence.timeZone ||
        (!this.props.recurrence.timeZone && timeZoneId === this.props.teamTimeZone);
    },

    updateSelectedTimeZone: function(timeZoneId, cityName, timeZoneName) {
      const newState = {
        selectedTimeZoneId: timeZoneId,
        selectedTimeZoneName: timeZoneName
      };
      if (!this.state.currentTimeZoneName && this.recurrenceTimeZoneMatches(timeZoneId)) {
        newState.currentTimeZoneName = newState.selectedTimeZoneName;
      }
      this.setState(newState);
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
                <span>{this.state.currentTimeZoneName}</span>
              ) : (
                <button type="button" className="button-raw" onClick={this.showTimeZones}>
                  <span className="type-black">{this.getCurrentTimeZoneName()}</span>
                  <span> — Modify</span>
                </button>
              )}
            </span>
          </div>
          <Collapsible revealWhen={this.shouldShowTimeZones()}>
            <TimeZoneSetter onChange={this.updateSelectedTimeZone} />
            <div className="mvm">
              <button type="button"
                className="button-s button-shrink mrs"
                disabled={!this.state.selectedTimeZoneId}
                onClick={this.setTimeZone}
              >Select time zone</button>
              <button type="button"
                className="button-s button-shrink"
                onClick={this.cancelSetTimeZone}
              >Cancel</button>
            </div>
          </Collapsible>
        </div>
      );
    }
  });
});
