define(function(require) {
  var React = require('react'),
    FormInput = require('../form/input'),
    MinuteInput = require('../form/minute_input'),
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
