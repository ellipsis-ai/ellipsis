define(function(require) {
var React = require('react');

return React.createClass({
  onMouseDown: function() {
    this.props.onClick();
  },

  onMouseUp: function() {
    this.blur();
  },

  blur: function() {
    this.refs.button.blur();
  },

  render: function() {
    return (
      <button type="button"
        className={
          "button-dropdown-trigger position-z-popup-trigger " +
          (this.props.openWhen ? "button-dropdown-trigger-menu-open" : "")
        }
        ref="button"
        onMouseDown={this.onMouseDown}
        onMouseUp={this.onMouseUp}
      >
        {React.Children.map(this.props.children, function(child) { return child; })}
      </button>
    );
  }
});

});
