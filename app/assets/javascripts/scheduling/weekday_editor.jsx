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

    onChangeDay: function(isChecked, stringValue) {
      const day = DayOfWeek.fromString(stringValue).value;
      if (isChecked && !this.weekdaysInclude(day)) {
        this.onChange(this.getWeekdays().concat(day));
      } else {
        this.onChange(this.getWeekdays().filter((ea) => ea !== day));
      }
    },

    render: function() {
      return (
        <div>
          <span className="align-button mrm">On</span>
          <span className="align-button">
            {DayOfWeek.WEEK.map((day) => (
              <Checkbox key={day.name()}
                className="mrm"
                checked={this.weekdaysInclude(day.value)}
                label={day.shortName()}
                title={day.name()}
                value={day.toString()}
                onChange={this.onChangeDay}
                useButtonStyle={true}
              />
            ))}
          </span>
        </div>
      );
    }
  });
});
