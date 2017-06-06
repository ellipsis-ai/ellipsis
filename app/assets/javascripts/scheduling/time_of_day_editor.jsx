define(function(require) {
  var React = require('react'),
    FormInput = require('../form/input'),
    MinuteInput = require('../form/minute_input'),
    ToggleGroup = require('../form/toggle_group'),
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
      if (typeof (hour) === "number") {
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
      if (typeof hour === "number") {
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
      } else if (typeof hour === "number") {
        newHour = hour;
      } else {
        newHour = 0;
      }
      this.setHour(newHour);
    },

    isPM: function() {
      const hour = this.getHour();
      if (typeof hour === "number") {
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
      } else if (typeof hour === "number") {
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
      } else if (typeof hour === "number") {
        str = hour.toString();
      }
      return str;
    },

    onChangeHour: function(newValue) {
      const parsed = newValue.substr(-2, 2).match(/^(1[0-2]|[1-9])$/) ||
        newValue.substr(-1, 1).match(/^([1-9])$/);
      let hour;
      if (parsed) {
        hour = parseInt(parsed[1], 10);
      }
      if (isNaN(hour)) {
        hour = null;
      } else if (this.isAM() && hour === 12) {
        hour = 0;
      } else if (this.isPM() && hour < 12) {
        hour += 12;
      }
      this.setHour(hour);
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
          <span className="align-button mlm">
            <ToggleGroup className="form-toggle-group-s">
              <ToggleGroup.Item onClick={this.setAM} label="AM" activeWhen={this.isAM()} />
              <ToggleGroup.Item onClick={this.setPM} label="PM" activeWhen={this.isPM()} />
            </ToggleGroup>
          </span>
          <span className="align-button mlm">
            {this.props.teamTimeZone}
          </span>
        </div>
      );
    }
  });
});
