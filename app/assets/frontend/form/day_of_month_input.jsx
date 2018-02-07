import * as React from 'react';
import FormInput from '../form/input';
import DayOfMonth from '../models/day_of_month';

const DayOfMonthInput = React.createClass({
    propTypes: {
      value: React.PropTypes.number,
      onChange: React.PropTypes.func.isRequired
    },

    onChange: function(newValue) {
      this.props.onChange(DayOfMonth.fromString(newValue).value);
    },

    getTextValue: function() {
      return new DayOfMonth(this.props.value).toString();
    },

    getOrdinalSuffix: function() {
      return new DayOfMonth(this.props.value).ordinalSuffix();
    },

    render: function() {
      return (
        <span>
          <FormInput
            className="width-2 form-input-borderless align-c"
            value={this.getTextValue()}
            onChange={this.onChange}
          />
          <span className="align-button type-label type-monospace">{this.getOrdinalSuffix()}</span>
        </span>
      );
    }
});

export default DayOfMonthInput;
