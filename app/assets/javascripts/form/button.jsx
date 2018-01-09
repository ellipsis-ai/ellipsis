// @flow
import type {ElementType} from 'react';
export type ButtonProps = {
  children: ElementType,
  className?: string,
  disabled?: boolean,
  onClick: () => void,
  title?: string
};

define(function(require) {
  const React = require('react'),
    autobind = require('../lib/autobind');

  type Props = ButtonProps;

  class Button extends React.PureComponent<Props> {
    props: Props;
    button: ?React.Element<'button'>;

    constructor(props) {
      super(props);
      autobind(this);
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

  return Button;
});
