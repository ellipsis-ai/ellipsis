import * as React from 'react';
import MinuteInput from '../form/minute_input';
import Recurrence from '../models/recurrence';

const MinuteOfHourEditor = React.createClass({
    propTypes: {
      recurrence: React.PropTypes.instanceOf(Recurrence).isRequired,
      onChange: React.PropTypes.func.isRequired
    },

    getValue: function() {
      return this.props.recurrence.minuteOfHour;
    },

    onChange: function(newValue) {
      this.props.onChange(this.props.recurrence.clone({
        minuteOfHour: newValue
      }));
    },

    getSuffix: function() {
      const value = this.getValue();
      if (!value) {
        return "(on the hour)";
      } else if (value === 1) {
        return "minute past the hour";
      } else {
        return "minutes past the hour";
      }
    },

    render: function() {
      return (
        <div>
          <span className="align-button mrm type-s">At</span>
          <MinuteInput value={this.getValue()} onChange={this.onChange} />
          <span className="align-button mlm type-s">{this.getSuffix()}</span>
        </div>
      );
    }
});

export default MinuteOfHourEditor;
