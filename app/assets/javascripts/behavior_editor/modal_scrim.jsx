define(function(require) {
  var React = require('react');

  return React.createClass({
    displayName: 'ModalScrim',
    propTypes: {
      isActive: React.PropTypes.bool.isRequired,
      onClick: React.PropTypes.func
    },

    onClick: function() {
      if (this.props.onClick) {
        this.props.onClick();
      }
    },

    getElement: function() {
      return this.refs.scrim;
    },

    render: function() {
      return (
        <div
          ref="scrim"
          className={
            "bg-scrim position-z-almost-front position-fixed-full " +
            (this.props.isActive ? "fade-in" : "display-none")
          }
          onClick={this.onClick}
        ></div>
      );
    }
  });
});
