define(function(require) {
  const React = require('react'),
    DataTypeField = require('../models/data_type_field'),
    Input = require('../form/input');

  class DataStorageAdderField extends React.Component {
    constructor(props) {
      super(props);
      this.input = null;
      this.onChange = this.onChange.bind(this);
    }

    onChange(newValue) {
      if (this.props.onChange) {
        this.props.onChange(newValue);
      }
    }

    focus() {
      if (this.input) {
        this.input.focus();
      }
    }

    render() {
      return (
        <div className="column-row">
          <div className="column column-shrink align-form-input type-s pvxs">
            {this.props.field.name ? (
              <span className="type-monospace">{this.props.field.name}</span>
            ) : (
              <span className="type-weak type-italic">Unnamed field</span>
            )}
          </div>
          <div className="column column-expand pvxs">
            <Input
              ref={(input) => this.input = input}
              value={this.props.value}
              onChange={this.onChange}
              onEnterKey={this.props.onEnterKey}
              className="form-input-borderless"
              readOnly={this.props.readOnly}
            />
          </div>
        </div>
      );
    }
  }

  DataStorageAdderField.propTypes = {
    field: React.PropTypes.instanceOf(DataTypeField).isRequired,
    value: React.PropTypes.string.isRequired,
    onChange: React.PropTypes.func,
    onEnterKey: React.PropTypes.func,
    readOnly: React.PropTypes.bool
  };

  return DataStorageAdderField;
});
