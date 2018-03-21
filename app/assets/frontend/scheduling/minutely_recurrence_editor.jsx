import * as React from 'react';
import FrequencyEditor from './frequency_editor';
import Recurrence from '../models/recurrence';

const MinutelyRecurrenceEditor = React.createClass({
    propTypes: {
      recurrence: React.PropTypes.instanceOf(Recurrence).isRequired,
      onChange: React.PropTypes.func.isRequired
    },

    render: function() {
      return (
        <div>
          <FrequencyEditor
            recurrence={this.props.recurrence}
            onChange={this.props.onChange}
            unit="minute"
            units="minutes"
            min={1}
            max={525600}
          />
        </div>
      );
    }
});

export default MinutelyRecurrenceEditor;
