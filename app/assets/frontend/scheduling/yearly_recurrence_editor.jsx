import * as React from 'react';
import FrequencyEditor from './frequency_editor';
import MonthDayEditor from './month_day_editor';
import TimeOfDayEditor from './time_of_day_editor';
import Recurrence from '../models/recurrence';

const YearlyRecurrenceEditor = React.createClass({
    propTypes: {
      recurrence: React.PropTypes.instanceOf(Recurrence).isRequired,
      onChange: React.PropTypes.func.isRequired,
      teamTimeZone: React.PropTypes.string.isRequired,
      teamTimeZoneName: React.PropTypes.string.isRequired
    },

    render: function() {
      return (
        <div>
          <div className="mvm">
            <FrequencyEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
              unit="year"
              units="years"
              min={1}
              max={10}
            />
          </div>
          <div className="mvm">
            <MonthDayEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
            />
          </div>
          <div className="mvm">
            <TimeOfDayEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
              teamTimeZone={this.props.teamTimeZone}
              teamTimeZoneName={this.props.teamTimeZoneName}
            />
          </div>
        </div>
      );
    }
});

export default YearlyRecurrenceEditor;
