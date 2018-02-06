import * as React from 'react';
import FrequencyEditor from './frequency_editor';
import MinuteOfHourEditor from './minute_of_hour_editor';
import Recurrence from '../models/recurrence';

const HourlyRecurrenceEditor = React.createClass({
    propTypes: {
      recurrence: React.PropTypes.instanceOf(Recurrence).isRequired,
      onChange: React.PropTypes.func.isRequired
    },

    render: function() {
      return (
        <div>
          <div className="mvm">
            <FrequencyEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
              unit="hour"
              units="hours"
              min={1}
              max={8760}
            />
          </div>
          <div className="mvm">
            <MinuteOfHourEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
            />
          </div>
        </div>
      );
    }
});

export default HourlyRecurrenceEditor;
