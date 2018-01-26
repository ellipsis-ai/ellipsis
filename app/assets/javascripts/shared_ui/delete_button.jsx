define(function(require) {
var React = require('react'),
  SVGXIcon = require('../svg/x');

return React.createClass({
  propTypes: {
    onClick: React.PropTypes.func.isRequired,
    title: React.PropTypes.string
  },
  onClick: function() {
    this.props.onClick();
    this.refs.button.blur();
  },

  render: function() {
    return (
      <span className="type-weak"><button type="button"
        ref="button"
        className="button-subtle button-symbol"
        onClick={this.onClick}
        title={this.props.title || "Delete"}
      >
        <SVGXIcon label="Delete" />
      </button></span>
    );
  }
});

});
