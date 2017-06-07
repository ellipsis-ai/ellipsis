define(function(require) {
  var React = require('react'),
    FormInput = require('../form/input'),
    Recurrence = require('../models/recurrence');

  return React.createClass({
    displayName: 'FrequencyEditor',
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
      const freq = this.getFrequency();
      if (Number.isInteger(freq)) {
        return freq.toString();
      } else {
        return "";
      }
    },

    getUnit: function() {
      return this.getFrequency() === 1 ? this.props.unit : this.props.units;
    },

    getValidInt: function(int) {
      if (isNaN(int)) {
        return this.props.min;
      } else if (int <= this.props.max) {
        return Math.max(int, this.props.min);
      } else {
        return Math.min(int, this.props.max);
      }
    },

    onChange: function(newValue) {
      const newFrequency = this.getValidInt(parseInt(newValue, 10));
      this.props.onChange(this.props.recurrence.clone({
        frequency: newFrequency
      }));
    },

    render: function() {
      return (
        <div>
          <span className="align-button mrm">Every</span>
          <FormInput
            className="width-5 form-input-borderless align-c"
            value={this.getTextValue()}
            onChange={this.onChange}
          />
          <span className="align-button mlm">{this.getUnit()}</span>
        </div>
      );
    }
  });
});
