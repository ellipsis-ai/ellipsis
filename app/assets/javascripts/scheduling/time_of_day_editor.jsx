define(function(require) {
  var React = require('react'),
    FormInput = require('../form/input'),
    MinuteInput = require('../form/minute_input'),
    ToggleGroup = require('../form/toggle_group'),
    OptionalInt = require('../models/optional_int'),
    Recurrence = require('../models/recurrence');

  return React.createClass({
    displayName: 'TimeOfDayEditor',
    propTypes: {
      recurrence: React.PropTypes.instanceOf(Recurrence).isRequired,
      onChange: React.PropTypes.func.isRequired,
      teamTimeZone: React.PropTypes.string.isRequired
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
      if (Number.isInteger(hour)) {
        return hour < 12;
      } else {
        return this.lastValidHour < 12;
      }
    },

    setAM: function() {
      const hour = this.getHour();
      let newHour;
      if (hour && hour >= 12) {
        newHour = hour - 12;
      } else if (Number.isInteger(hour)) {
        newHour = hour;
      } else {
        newHour = 0;
      }
      this.setHour(newHour);
    },

    isPM: function() {
      const hour = this.getHour();
      if (Number.isInteger(hour)) {
        return hour >= 12;
      } else {
        return this.lastValidHour >= 12;
      }
    },

    setPM: function() {
      const hour = this.getHour();
      let newHour;
      if (hour && hour < 12) {
        newHour = hour + 12;
      } else if (Number.isInteger(hour)) {
        newHour = hour;
      } else {
        newHour = 12;
      }
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
      let str = "";
      if (hour > 12) {
        str = (hour - 12).toString();
      } else if (hour === 0) {
        str = "12";
      } else if (Number.isInteger(hour)) {
        str = hour.toString();
      }
      return str;
    },

    onChangeHour: function(newValue) {
      const parsed = newValue.substr(-2, 2).match(/^(1[0-2]|[1-9])$/) ||
        newValue.substr(-1, 1).match(/^([1-9])$/);
      const hour = OptionalInt.fromString(parsed ? parsed[1] : "");
      let adjustedValue;
      if (this.isAM() && hour.is((int) => int === 12)) {
        adjustedValue = 0;
      } else if (this.isPM() && hour.is((int) => int < 12)) {
        adjustedValue = hour.value + 12;
      } else {
        adjustedValue = hour.value;
      }
      this.setHour(adjustedValue);
    },

    render: function() {
      return (
        <div>
          <span className="align-button mrm">At</span>
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
          <span className="align-button">
            {this.props.teamTimeZone}
          </span>
        </div>
      );
    }
  });
});
