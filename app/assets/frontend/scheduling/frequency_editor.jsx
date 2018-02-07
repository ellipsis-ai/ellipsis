import * as React from 'react';
import FormInput from '../form/input';
import OptionalInt from '../models/optional_int';
import Recurrence from '../models/recurrence';

const FrequencyEditor = React.createClass({
    propTypes: {
      recurrence: React.PropTypes.instanceOf(Recurrence).isRequired,
      onChange: React.PropTypes.func.isRequired,
      unit: React.PropTypes.string.isRequired,
      units: React.PropTypes.string.isRequired,
      min: React.PropTypes.number.isRequired,
      max: React.PropTypes.number.isRequired
    },

    getFrequency: function() {
      return this.props.recurrence.frequency;
    },

    getTextValue: function() {
      return new OptionalInt(this.getFrequency()).toString();
    },

    getUnit: function() {
      return this.getFrequency() === 1 ? this.props.unit : this.props.units;
    },

    onChange: function(newValue) {
      const newFrequency = OptionalInt.fromString(newValue).valueWithinRange(this.props.min, this.props.max);
      this.props.onChange(this.props.recurrence.clone({
        frequency: newFrequency
      }));
    },

    render: function() {
      return (
        <div>
          <span className="align-button mrm type-s">Every</span>
          <FormInput
            className="width-5 form-input-borderless align-c"
            value={this.getTextValue()}
            onChange={this.onChange}
          />
          <span className="align-button mlm type-s">{this.getUnit()}</span>
        </div>
      );
    }
});

export default FrequencyEditor;
