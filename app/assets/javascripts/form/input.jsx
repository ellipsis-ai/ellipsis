define(function(require) {
var React = require('react'),
  Event = require('../lib/event');

return React.createClass({
  displayName: 'FormInput',
  propTypes: {
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
    disableAuto: React.PropTypes.bool
  },

  onChange: function() {
    this.props.onChange(this.refs.input.value);
  },

  onBlur: function() {
    if (typeof(this.props.onBlur) === 'function') {
      this.props.onBlur(this.refs.input.value);
    }
  },

  onFocus: function() {
    if (typeof(this.props.onFocus) === 'function') {
      this.props.onFocus(this.refs.input.value);
    }
  },

  handleKeyPress: function(event) {
    if (Event.keyPressWasEnter(event)) {
      event.preventDefault();
      if (typeof this.props.onEnterKey === 'function') {
        this.props.onEnterKey();
      }
    }
  },

  handleKeyDown: function(event) {
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
  },

  isEmpty: function() {
    return !this.refs.input.value;
  },

  focus: function() {
    this.refs.input.focus();
  },

  blur: function() {
    this.refs.input.blur();
  },

  select: function() {
    this.refs.input.select();
  },

  componentDidMount: function() {
    // Need to add keystroke handlers directly with the DOM, because React events don't bubble up all the way
    this.refs.input.addEventListener('keydown', this.handleKeyDown, false);
    this.refs.input.addEventListener('keypress', this.handleKeyPress, false);
  },

  render: function() {
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
      />
    );
  }
});

});
