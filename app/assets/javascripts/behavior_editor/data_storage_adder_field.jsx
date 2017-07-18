define(function(require) {
  const React = require('react'),
    Input = require('../form/input'),
    autobind = require('../lib/autobind');

  class DataStorageAdderField extends React.Component {
    constructor(props) {
      super(props);
      this.input = null;
      autobind(this);
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
            {this.props.name ? (
              <span className="type-monospace">{this.props.name}</span>
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
    name: React.PropTypes.string,
    value: React.PropTypes.string.isRequired,
    onChange: React.PropTypes.func,
    onEnterKey: React.PropTypes.func,
    readOnly: React.PropTypes.bool
  };

  return DataStorageAdderField;
});
