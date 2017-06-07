define(function(require) {
  var React = require('react'),
    Checkbox = require('../form/checkbox'),
    Recurrence = require('../models/recurrence');

  const SUNDAY = 0;
  const MONDAY = 1;
  const TUESDAY = 2;
  const WEDNESDAY = 3;
  const THURSDAY = 4;
  const FRIDAY = 5;
  const SATURDAY = 6;

  return React.createClass({
    displayName: 'WeekdayEditor',
    propTypes: {
      recurrence: React.PropTypes.instanceOf(Recurrence).isRequired,
      onChange: React.PropTypes.func.isRequired
    },

    getWeekdays: function() {
      return this.props.recurrence.daysOfWeek;
    },

    onChange: function(newWeekdays) {
      this.props.onChange(this.props.recurrence.clone({
        daysOfWeek: newWeekdays
      }));
    },

    weekdaysInclude: function(day) {
      return this.getWeekdays().includes(day);
    },

    toggleDay: function(day) {
      if (this.weekdaysInclude(day)) {
        this.onChange(this.getWeekdays().filter((ea) => ea !== day));
      } else {
        this.onChange(this.getWeekdays().concat(day));
      }
    },

    toggleMonday: function() {
      this.toggleDay(MONDAY);
    },

    toggleTuesday: function() {
      this.toggleDay(TUESDAY);
    },

    toggleWednesday: function() {
      this.toggleDay(WEDNESDAY);
    },

    toggleThursday: function() {
      this.toggleDay(THURSDAY);
    },

    toggleFriday: function() {
      this.toggleDay(FRIDAY);
    },

    toggleSaturday: function() {
      this.toggleDay(SATURDAY);
    },

    toggleSunday: function() {
      this.toggleDay(SUNDAY);
    },

    render: function() {
      return (
        <div>
          <span className="align-button mrm">On</span>
          <span className="align-button">
            <Checkbox className="mrm"
              checked={this.weekdaysInclude(MONDAY)}
              label="Mon"
              title="Monday"
              onChange={this.toggleMonday}
              useButtonStyle={true}
            />
            <Checkbox className="mrm"
              checked={this.weekdaysInclude(TUESDAY)}
              label="Tue"
              title="Tuesday"
              onChange={this.toggleTuesday}
              useButtonStyle={true}
            />
            <Checkbox className="mrm"
              checked={this.weekdaysInclude(WEDNESDAY)}
              label="Wed"
              title="Wednesday"
              onChange={this.toggleWednesday}
              useButtonStyle={true}
            />
            <Checkbox className="mrm"
              checked={this.weekdaysInclude(THURSDAY)}
              label="Thu"
              title="Thursday"
              onChange={this.toggleThursday}
              useButtonStyle={true}
            />
            <Checkbox className="mrm"
              checked={this.weekdaysInclude(FRIDAY)}
              label="Fri"
              title="Friday"
              onChange={this.toggleFriday}
              useButtonStyle={true}
            />
            <Checkbox className="mrm"
              checked={this.weekdaysInclude(SATURDAY)}
              label="Sat"
              title="Saturday"
              onChange={this.toggleSaturday}
              useButtonStyle={true}
            />
            <Checkbox className=""
              checked={this.weekdaysInclude(SUNDAY)}
              label="Sun"
              title="Sunday"
              onChange={this.toggleSunday}
              useButtonStyle={true}
            />
          </span>
        </div>
      );
    }
  });
});
