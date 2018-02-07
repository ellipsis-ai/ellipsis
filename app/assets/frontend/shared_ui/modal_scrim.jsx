import * as React from 'react';

const ModalScrim = React.createClass({
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

    shouldComponentUpdate: function(newProps) {
      return this.props.isActive !== newProps.isActive;
    },

    render: function() {
      return (
        <div
          ref="scrim"
          className={
            "bg-scrim position-z-scrim position-fixed-full " +
            (this.props.isActive ? "fade-in" : "display-none")
          }
          onClick={this.onClick}
        />
      );
    }
});

export default ModalScrim;