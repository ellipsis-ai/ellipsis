define(function(require) {
  const React = require('react');

  class Button extends React.Component {
    constructor(props) {
      super(props);
      this.onClick = this.onClick.bind(this);
      this.button = null;
    }

    onClick() {
      // Strip the browser event object from the onClick handler
      if (this.props.onClick) {
        this.props.onClick();
      }
    }

    blur() {
      if (this.button) {
        this.button.blur();
      }
    }

    render() {
      return (
        <button
          ref={(el) => this.button = el}
          className={
            `button ${
              this.props.className || ""
            }`
          }
          type="button"
          onClick={this.onClick}
          disabled={this.props.disabled}
          title={this.props.title}
        >{this.props.children}</button>
      );
    }
  }

  Button.propTypes = {
    children: React.PropTypes.node.isRequired,
    className: React.PropTypes.string,
    disabled: React.PropTypes.bool,
    onClick: React.PropTypes.func.isRequired,
    title: React.PropTypes.string
  };

  return Button;
});
