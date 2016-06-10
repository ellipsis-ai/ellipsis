define(function(require) {
var React = require('react');

return React.createClass({
  onChange: function() {
    this.props.onChange(!!this.refs.input.checked);
  },

  handleEnterKey: function(event) {
    if (event.which == 13) {
      event.preventDefault();
      if (typeof this.props.onEnterKey == 'function') {
        this.props.onEnterKey();
      }
    }
  },

  focus: function() {
    this.refs.input.focus();
  },

  render: function() {
    return (
      <input type="checkbox"
        ref="input"
        checked={this.props.checked}
        onChange={this.onChange}
        onKeyPress={this.handleEnterKey}
      />
    );
  }
});

});
