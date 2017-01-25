define(function(require) {
var React = require('react'),
  CSS = require('../lib/css'),
  SVGXIcon = require('../svg/x');

return React.createClass({
  propTypes: {
    hidden: React.PropTypes.bool,
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
        className={"button-subtle button-symbol" + CSS.visibleWhen(!this.props.hidden)}
        onClick={this.onClick}
        title={this.props.title || "Delete"}
      >
        <SVGXIcon label="Delete" />
      </button></span>
    );
  }
});

});
