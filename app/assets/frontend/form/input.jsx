import * as React from 'react';
import Event from '../lib/event';

class FormInput extends React.Component {

  constructor(props) {
    super(props);
    ['onChange', 'onBlur', 'onFocus', 'handleKeyPress', 'handleKeyDown'].forEach((func) => this[func] = this[func].bind(this));
  }

  onChange() {
    this.props.onChange(this.refs.input.value);
  }

  onBlur() {
    if (typeof(this.props.onBlur) === 'function') {
      this.props.onBlur(this.refs.input.value);
    }
  }

  onFocus() {
    if (typeof(this.props.onFocus) === 'function') {
      this.props.onFocus(this.refs.input.value);
    }
  }

  handleKeyPress(event) {
    if (Event.keyPressWasEnter(event)) {
      event.preventDefault();
      if (typeof this.props.onEnterKey === 'function') {
        this.props.onEnterKey();
      }
    }
  }

  handleKeyDown(event) {
    if (Event.keyPressWasEsc(event) && this.props.onEscKey) {
      event.stopPropagation();
      event.preventDefault();
      this.props.onEscKey();
    } else if (Event.keyPressWasDown(event) && typeof this.props.onDownKey === 'function') {
      event.preventDefault();
      this.props.onDownKey();
    } else if (Event.keyPressWasUp(event) && typeof this.props.onUpKey === 'function') {
      event.preventDefault();
      this.props.onUpKey();
    }
  }

  isEmpty() {
    return !this.refs.input.value;
  }

  focus() {
    setTimeout(() => {
      this.refs.input.focus();
    }, 1);
  }

  blur() {
    this.refs.input.blur();
  }

  select() {
    this.refs.input.select();
  }

  componentDidMount() {
    // Need to add keystroke handlers directly with the DOM, because React events don't bubble up all the way
    this.refs.input.addEventListener('keydown', this.handleKeyDown, false);
    this.refs.input.addEventListener('keypress', this.handleKeyPress, false);
  }

  render() {
    return (
      <input
        type={this.props.type || "text"}
        className={"form-input " + (this.props.className || "")}
        style={this.props.style}
        ref="input"
        id={this.props.id}
        name={this.props.name}
        value={this.props.value}
        placeholder={this.props.placeholder}
        autoFocus={this.props.autoFocus}
        onChange={this.onChange}
        onBlur={this.onBlur}
        onFocus={this.onFocus}
        autoCapitalize={this.props.disableAuto ? "off" : null}
        autoComplete={this.props.disableAuto ? "off" : null}
        autoCorrect={this.props.disableAuto ? "off" : null}
        spellCheck={this.props.disableAuto ? false : null}
        disabled={!!this.props.disabled}
        readOnly={!!this.props.readOnly}
      />
    );
  }
}

FormInput.propTypes = {
  autoFocus: React.PropTypes.bool,
  className: React.PropTypes.string,
  style: React.PropTypes.object,
  id: React.PropTypes.oneOfType([
    React.PropTypes.number,
    React.PropTypes.string
  ]),
  name: React.PropTypes.string,
  onBlur: React.PropTypes.func,
  onChange: React.PropTypes.func.isRequired,
  onEnterKey: React.PropTypes.func,
  onEscKey: React.PropTypes.func,
  onFocus: React.PropTypes.func,
  onDownKey: React.PropTypes.func,
  onUpKey: React.PropTypes.func,
  placeholder: React.PropTypes.string,
  type: React.PropTypes.string,
  value: React.PropTypes.string.isRequired,
  disableAuto: React.PropTypes.bool,
  disabled: React.PropTypes.bool,
  readOnly: React.PropTypes.bool
};

export default FormInput;
