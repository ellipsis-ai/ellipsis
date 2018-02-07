import * as React from 'react';
import SVGXIcon from '../svg/x';

const DeleteButton = React.createClass({
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

export default DeleteButton;
