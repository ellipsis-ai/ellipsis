define(function(require) {
  const React = require('react'),
    FormInput = require('../form/input'),
    ParamType = require('../models/param_type'),
    Select = require('../form/select'),
    autobind = require('../lib/autobind');

  class DataStorageAdderField extends React.Component {
    constructor(props) {
      super(props);
      this.input = null;
      autobind(this);
    }

    onChange(value) {
      if (this.props.onChange) {
        const newValue = this.props.fieldType ? this.props.fieldType.formatValue(value) : value;
        this.props.onChange(newValue);
      }
    }

    focus() {
      if (this.input) {
        this.input.focus();
      }
    }

    getPlaceholder() {
      return this.props.fieldType ? this.props.fieldType.getInputPlaceholder() : null;
    }

    renderInput() {
      const options = this.props.fieldType ? this.props.fieldType.getOptions() : null;
      if (options && !this.props.readOnly) {
        return (
          <div className="align-form-input">
            <Select
              className="form-select-s align-m"
              ref={(input) => this.input = input}
              value={this.props.value}
              onChange={this.onChange}
            >
              {options.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </Select>
          </div>
        );
      } else {
        return (
          <FormInput
            ref={(input) => this.input = input}
            value={this.props.value}
            onChange={this.onChange}
            onEnterKey={this.props.onEnterKey}
            className="form-input-borderless"
            readOnly={this.props.readOnly}
            placeholder={this.getPlaceholder()}
          />
        );
      }
    }

    render() {
      return (
        <div className="column-row">
          <div className="column column-shrink align-form-input type-s pvxs">
            {this.props.name ? (
              <span className="type-monospace">{this.props.name}</span>
            ) : (
              <span className="type-weak type-italic">Unnamed field</span>
            )}
          </div>
          <div className="column column-expand pvxs">
            {this.renderInput()}
          </div>
        </div>
      );
    }
  }

  DataStorageAdderField.propTypes = {
    name: React.PropTypes.string,
    value: React.PropTypes.string.isRequired,
    fieldType: React.PropTypes.instanceOf(ParamType),
    onChange: React.PropTypes.func,
    onEnterKey: React.PropTypes.func,
    readOnly: React.PropTypes.bool
  };

  return DataStorageAdderField;
});
