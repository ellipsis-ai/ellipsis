define(function(require) {
  const React = require('react');

  class Button extends React.Component {
    constructor(props) {
      super(props);
      this.onClick = this.onClick.bind(this);
    }

    onClick() {
      // Strip the browser event object from the onClick handler
      if (this.props.onClick) {
        this.props.onClick();
      }
    }

    render() {
      return (
        <button
          className={
            `button ${
              this.props.className || ""
            }`
          }
          type="button"
          onClick={this.onClick}
          disabled={this.props.disabled}
        >{this.props.children}</button>
      );
    }
  }

  Button.propTypes = {
    children: React.PropTypes.node.isRequired,
    className: React.PropTypes.string,
    disabled: React.PropTypes.bool,
    onClick: React.PropTypes.func.isRequired
  };

  return Button;
});
