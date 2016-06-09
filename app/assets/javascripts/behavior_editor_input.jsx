define(function(require) {
var React = require('react');

return React.createClass({
  displayName: 'BehaviorEditorInput',
  onChange: function() {
    this.props.onChange(this.refs.input.value);
  },

  handleEnterKey: function(event) {
    if (event.which == 13) {
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

  select: function() {
    this.refs.input.select();
  },

  render: function() {
    return (
      <input type="text"
        className={"form-input " + (this.props.className || "")}
        ref="input"
        id={this.props.id}
        value={this.props.value}
        placeholder={this.props.placeholder}
        autoFocus={this.props.autoFocus}
        onChange={this.onChange}
        onKeyPress={this.handleEnterKey}
      />
    );
  }
});

});
