define(function(require) {
var React = require('react');

return React.createClass({
  propTypes: {
    checked: React.PropTypes.bool.isRequired,
    onChange: React.PropTypes.func.isRequired,
    onEnterKey: React.PropTypes.func,
    className: React.PropTypes.string,
    label: React.PropTypes.string,
    name: React.PropTypes.string
  },
  onChange: function() {
    this.props.onChange(!!this.refs.input.checked);
  },

  handleEnterKey: function(event) {
    if (event.which === 13) {
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
      <label className={this.props.className || ""}>
        <input type="checkbox"
          className={this.props.label ? "man mrs" : "man"}
          ref="input"
          checked={this.props.checked}
          onChange={this.onChange}
          onKeyPress={this.handleEnterKey}
          name={this.props.name}
        />
        <span>{this.props.label}</span>
      </label>
    );
  }
});

});
