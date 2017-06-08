define(function(require) {
  var React = require('react'),
    Checkbox = require('../form/checkbox'),
    DayOfWeek = require('../models/day_of_week'),
    Recurrence = require('../models/recurrence');

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
      this.toggleDay(DayOfWeek.MONDAY.value);
    },

    toggleTuesday: function() {
      this.toggleDay(DayOfWeek.TUESDAY.value);
    },

    toggleWednesday: function() {
      this.toggleDay(DayOfWeek.WEDNESDAY.value);
    },

    toggleThursday: function() {
      this.toggleDay(DayOfWeek.THURSDAY.value);
    },

    toggleFriday: function() {
      this.toggleDay(DayOfWeek.FRIDAY.value);
    },

    toggleSaturday: function() {
      this.toggleDay(DayOfWeek.SATURDAY.value);
    },

    toggleSunday: function() {
      this.toggleDay(DayOfWeek.SUNDAY.value);
    },

    render: function() {
      return (
        <div>
          <span className="align-button mrm">On</span>
          <span className="align-button">
            <Checkbox className="mrm"
              checked={this.weekdaysInclude(DayOfWeek.MONDAY.value)}
              label={DayOfWeek.MONDAY.shortName()}
              title={DayOfWeek.MONDAY.name()}
              onChange={this.toggleMonday}
              useButtonStyle={true}
            />
            <Checkbox className="mrm"
              checked={this.weekdaysInclude(DayOfWeek.TUESDAY.value)}
              label={DayOfWeek.TUESDAY.shortName()}
              title={DayOfWeek.TUESDAY.name()}
              onChange={this.toggleTuesday}
              useButtonStyle={true}
            />
            <Checkbox className="mrm"
              checked={this.weekdaysInclude(DayOfWeek.WEDNESDAY.value)}
              label={DayOfWeek.WEDNESDAY.shortName()}
              title={DayOfWeek.WEDNESDAY.name()}
              onChange={this.toggleWednesday}
              useButtonStyle={true}
            />
            <Checkbox className="mrm"
              checked={this.weekdaysInclude(DayOfWeek.THURSDAY.value)}
              label={DayOfWeek.THURSDAY.shortName()}
              title={DayOfWeek.THURSDAY.name()}
              onChange={this.toggleThursday}
              useButtonStyle={true}
            />
            <Checkbox className="mrm"
              checked={this.weekdaysInclude(DayOfWeek.FRIDAY.value)}
              label={DayOfWeek.FRIDAY.shortName()}
              title={DayOfWeek.FRIDAY.name()}
              onChange={this.toggleFriday}
              useButtonStyle={true}
            />
            <Checkbox className="mrm"
              checked={this.weekdaysInclude(DayOfWeek.SATURDAY.value)}
              label={DayOfWeek.SATURDAY.shortName()}
              title={DayOfWeek.SATURDAY.name()}
              onChange={this.toggleSaturday}
              useButtonStyle={true}
            />
            <Checkbox className=""
              checked={this.weekdaysInclude(DayOfWeek.SUNDAY.value)}
              label={DayOfWeek.SUNDAY.shortName()}
              title={DayOfWeek.SUNDAY.name()}
              onChange={this.toggleSunday}
              useButtonStyle={true}
            />
          </span>
        </div>
      );
    }
  });
});
