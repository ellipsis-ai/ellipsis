define(function(require) {
  var React = require('react'),
    autobind = require('../lib/autobind');

  class Textarea extends React.Component {
    constructor(props) {
      super(props);
      autobind(this);
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

    isEmpty() {
      return !this.refs.input.value;
    }

    focus() {
      this.refs.input.focus();
    }

    select() {
      this.refs.input.select();
    }

    render() {
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
          rows={this.props.rows}
        />
      );
    }
  }

  Textarea.propTypes = {
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
    rows: React.PropTypes.string,
    value: React.PropTypes.string.isRequired
  };

  return Textarea;

});
