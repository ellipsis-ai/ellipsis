import * as React from 'react';
export interface ButtonProps {
  children: any,
  className?: string,
  disabled?: boolean,
  onClick: () => void,
  title?: string
}

import autobind from '../lib/autobind';

type Props = ButtonProps;

class Button extends React.PureComponent<Props> {
    props: Props;
    button: HTMLButtonElement | null;

    constructor(props: Props) {
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

export default Button;

