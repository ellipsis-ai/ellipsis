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
      onBlur: React.PropTypes.func,
      onChange: React.PropTypes.func.isRequired,
      onFocus: React.PropTypes.func,
      placeholder: React.PropTypes.string,
      size: React.PropTypes.string,
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

    isEmpty: function() {
      return !this.refs.input.value;
    },

    focus: function() {
      this.refs.input.focus();
    },

    select: function() {
      this.refs.input.select();
    },

    render: function() {
      return (
        <textarea
          className={"form-input " + (this.props.className || "")}
          ref="input"
          id={this.props.id}
          placeholder={this.props.placeholder}
          value={this.props.value}
          autoFocus={this.props.autoFocus}
          onChange={this.onChange}
          onBlur={this.onBlur}
          onFocus={this.onFocus}
        />
      );
    }
  });

});
