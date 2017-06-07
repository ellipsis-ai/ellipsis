define(function(require) {
  var React = require('react'),
    FormInput = require('./input'),
    Minute = require('../models/minute');

  return React.createClass({
    displayName: 'MinuteInput',
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
});
