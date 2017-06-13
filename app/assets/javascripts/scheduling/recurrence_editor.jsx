define(function(require) {
  var React = require('react'),
    ToggleGroup = require('../form/toggle_group'),
    Recurrence = require('../models/recurrence'),
    MinutelyRecurrenceEditor = require('./minutely_recurrence_editor'),
    HourlyRecurrenceEditor = require('./hourly_recurrence_editor'),
    DailyRecurrenceEditor = require('./daily_recurrence_editor'),
    WeeklyRecurrenceEditor = require('./weekly_recurrence_editor'),
    MonthlyRecurrenceEditor = require('./monthly_recurrence_editor'),
    YearlyRecurrenceEditor = require('./yearly_recurrence_editor');

  return React.createClass({
    displayName: 'RecurrenceEditor',
    propTypes: {
      recurrence: React.PropTypes.instanceOf(Recurrence).isRequired,
      onChange: React.PropTypes.func.isRequired,
      teamTimeZone: React.PropTypes.string.isRequired,
      teamTimeZoneName: React.PropTypes.string.isRequired
    },

    typeMatches: function(typeName) {
      return this.props.recurrence.typeName.indexOf(typeName) === 0;
    },

    set: function(newProps) {
      this.props.onChange(this.props.recurrence.clone(newProps));
    },

    setTypeMinutely: function() {
      this.props.onChange(this.props.recurrence.becomeMinutely());
    },

    setTypeHourly: function() {
      this.props.onChange(this.props.recurrence.becomeHourly());
    },

    setTypeDaily: function() {
      this.props.onChange(this.props.recurrence.becomeDaily({
        timeZone: this.props.teamTimeZone
      }));
    },

    setTypeWeekly: function() {
      this.props.onChange(this.props.recurrence.becomeWeekly({
        timeZone: this.props.teamTimeZone
      }));
    },

    setTypeMonthly: function() {
      this.props.onChange(this.props.recurrence.becomeMonthlyByDayOfMonth({
        timeZone: this.props.teamTimeZone
      }));
    },

    setTypeYearly: function() {
      this.props.onChange(this.props.recurrence.becomeYearly({
        timeZone: this.props.teamTimeZone
      }));
    },

    renderRecurrenceEditorForType: function() {
      if (this.typeMatches("yearly")) {
        return (
          <YearlyRecurrenceEditor recurrence={this.props.recurrence}
            onChange={this.props.onChange}
            teamTimeZone={this.props.teamTimeZone}
            teamTimeZoneName={this.props.teamTimeZoneName}
          />
        );
      } else if (this.typeMatches("monthly")) {
        return (
          <MonthlyRecurrenceEditor recurrence={this.props.recurrence}
            onChange={this.props.onChange}
            teamTimeZone={this.props.teamTimeZone}
            teamTimeZoneName={this.props.teamTimeZoneName}
          />
        );
      } else if (this.typeMatches("weekly")) {
        return (
          <WeeklyRecurrenceEditor recurrence={this.props.recurrence}
            onChange={this.props.onChange}
            teamTimeZone={this.props.teamTimeZone}
            teamTimeZoneName={this.props.teamTimeZoneName}
          />
        );
      } else if (this.typeMatches("daily")) {
        return (
          <DailyRecurrenceEditor recurrence={this.props.recurrence}
            onChange={this.props.onChange}
            teamTimeZone={this.props.teamTimeZone}
            teamTimeZoneName={this.props.teamTimeZoneName}
          />
        );
      } else if (this.typeMatches("hourly")) {
        return (
          <HourlyRecurrenceEditor recurrence={this.props.recurrence} onChange={this.props.onChange}/>
        );
      } else { /* this.typeMatches("minutely") or any future unknown type */
        return (
          <MinutelyRecurrenceEditor recurrence={this.props.recurrence} onChange={this.props.onChange}/>
        );
      }
    },

    render: function() {
      return (
        <div>
          <div className="mvm">
            <div className="align-button mrm type-s">Repeat</div>
            <div className="align-button">
              <ToggleGroup className="form-toggle-group-s">
                <ToggleGroup.Item
                  onClick={this.setTypeMinutely}
                  activeWhen={this.typeMatches("minutely")}
                  label="Minutely"
                />
                <ToggleGroup.Item
                  onClick={this.setTypeHourly}
                  activeWhen={this.typeMatches("hourly")}
                  label="Hourly"
                />
                <ToggleGroup.Item
                  onClick={this.setTypeDaily}
                  activeWhen={this.typeMatches("daily")}
                  label="Daily"
                />
                <ToggleGroup.Item
                  onClick={this.setTypeWeekly}
                  activeWhen={this.typeMatches("weekly")}
                  label="Weekly"
                />
                <ToggleGroup.Item
                  onClick={this.setTypeMonthly}
                  activeWhen={this.typeMatches("monthly")}
                  label="Monthly"
                />
                <ToggleGroup.Item
                  onClick={this.setTypeYearly}
                  activeWhen={this.typeMatches("yearly")}
                  label="Yearly"
                />
              </ToggleGroup>
            </div>
          </div>

          <div className="mvm">
            {this.renderRecurrenceEditorForType()}
          </div>
        </div>
      );
    }
  });
});
