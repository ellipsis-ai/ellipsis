define(function(require) {
  const React = require('react'),
    DataTypeField = require('../models/data_type_field'),
    Input = require('../form/input');

  class DataStorageAdderField extends React.Component {
    constructor(props) {
      super(props);
      this.onChange = this.onChange.bind(this);
    }

    onChange(newValue) {
      this.props.onChange(newValue);
    }

    render() {
      return (
        <div className="column-row">
          <div className="column column-shrink align-form-input type-s">
            {this.props.field.name ? (
              <span className="type-monospace">{this.props.field.name}</span>
            ) : (
              <span className="type-weak type-italic">Unnamed field</span>
            )}
          </div>
          <div className="column column-expand pbm">
            <Input value={this.props.value} onChange={this.onChange} className="form-input-borderless" />
          </div>
        </div>
      );
    }
  }

  DataStorageAdderField.propTypes = {
    field: React.PropTypes.instanceOf(DataTypeField).isRequired,
    value: React.PropTypes.string.isRequired,
    onChange: React.PropTypes.func.isRequired
  };

  return DataStorageAdderField;
});
