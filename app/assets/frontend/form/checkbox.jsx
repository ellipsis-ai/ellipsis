import * as React from 'react';

const Checkbox = React.createClass({
  propTypes: {
    checked: React.PropTypes.bool.isRequired,
    onChange: React.PropTypes.func.isRequired,
    onEnterKey: React.PropTypes.func,
    className: React.PropTypes.string,
    label: React.PropTypes.string,
    title: React.PropTypes.string,
    name: React.PropTypes.string,
    useButtonStyle: React.PropTypes.bool,
    value: React.PropTypes.string,
    disabledWhen: React.PropTypes.bool
  },
  onChange: function() {
    this.props.onChange(Boolean(this.refs.input.checked), this.props.value);
  },

  handleEnterKey: function(event) {
    if (event.which === 13) {
      event.preventDefault();
      if (typeof this.props.onEnterKey === 'function') {
        this.props.onEnterKey();
      }
    }
  },

  focus: function() {
    this.refs.input.focus();
  },

  getClassName: function() {
    const classNames = [];
    if (this.props.useButtonStyle) {
      if (this.props.checked) {
        classNames.push("checkbox-button checkbox-button-s checkbox-button-checked");
      } else {
        classNames.push("checkbox-button checkbox-button-s");
      }
    }
    if (this.props.className) {
      classNames.push(this.props.className);
    }
    return classNames.join(" ");
  },

  render: function() {
    return (
      <label className={this.getClassName()} title={this.props.title}>
        <input type="checkbox"
          className={this.props.label ? "man mrs" : "man"}
          ref="input"
          checked={this.props.checked}
          onChange={this.onChange}
          onKeyPress={this.handleEnterKey}
          name={this.props.name}
          value={this.props.value}
          disabled={this.props.disabledWhen}
        />
        <span>{this.props.label}</span>
      </label>
    );
  }
});

export default Checkbox;
