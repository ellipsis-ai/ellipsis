define(function(require) {
var React = require('react');

return React.createClass({
  propTypes: {
    autoFocus: React.PropTypes.bool,
    className: React.PropTypes.string,
    id: React.PropTypes.oneOfType([
      React.PropTypes.number,
      React.PropTypes.string
    ]),
    name: React.PropTypes.string,
    onBlur: React.PropTypes.func,
    onChange: React.PropTypes.func.isRequired,
    onEnterKey: React.PropTypes.func,
    onFocus: React.PropTypes.func,
    placeholder: React.PropTypes.string,
    type: React.PropTypes.string,
    value: React.PropTypes.string.isRequired
  },

  onChange: function() {
    this.props.onChange(this.refs.input.value);
  },

  onBlur: function() {
    if (typeof(this.props.onBlur) == 'function') {
      this.props.onBlur(this.refs.input.value);
    }
  },

  onFocus: function() {
    if (typeof(this.props.onFocus) == 'function') {
      this.props.onFocus(this.refs.input.value);
    }
  },

  handleEnterKey: function(event) {
    if (event.which === 13) {
      event.preventDefault();
      if (typeof this.props.onEnterKey == 'function') {
        this.props.onEnterKey();
      }
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

  render: function() {
    return (
      <input
        type={this.props.type || "text"}
        className={"form-input " + (this.props.className || "")}
        ref="input"
        id={this.props.id}
        value={this.props.value}
        placeholder={this.props.placeholder}
        autoFocus={this.props.autoFocus}
        onChange={this.onChange}
        onBlur={this.onBlur}
        onFocus={this.onFocus}
        onKeyPress={this.handleEnterKey}
      />
    );
  }
});

});
