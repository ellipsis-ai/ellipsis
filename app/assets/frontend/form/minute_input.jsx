import * as React from 'react';
import FormInput from '../../javascripts/form/input';
import Minute from '../models/minute';

const MinuteInput = React.createClass({
    propTypes: {
      value: React.PropTypes.number,
      onChange: React.PropTypes.func.isRequired
    },

    getTextValue: function() {
      return new Minute(this.props.value).toString();
    },

    onChange: function(newValue) {
      this.props.onChange(Minute.fromString(newValue).value);
    },

    render: function() {
      return (
        <span>
          <span className="align-button">:</span>
          <FormInput
            className="width-2 form-input-borderless align-c"
            value={this.getTextValue()}
            onChange={this.onChange}
          />
        </span>
      );
    }
});

export default MinuteInput;
