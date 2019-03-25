import * as React from 'react';
import autobind from '../lib/autobind';
import {CSSProperties} from "react";

export interface ButtonProps {
  children: any,
  className?: string,
  disabled?: boolean,
  onClick: Option<() => void>,
  title?: string,
  style?: Partial<CSSProperties>,
  stopPropagation?: boolean
}

type Props = ButtonProps;

class Button extends React.PureComponent<Props> {
    props: Props;
    button: Option<HTMLButtonElement>;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.button = null;
    }

    componentDidMount(): void {
      if (this.button) {
        this.button.addEventListener('click', this.internalClickHandler);
      }
    }

    internalClickHandler(event: MouseEvent): void {
      if (this.props.stopPropagation) {
        event.stopPropagation();
      }
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
          disabled={this.props.disabled}
          title={this.props.title}
        >{this.props.children}</button>
      );
    }
}

export default Button;

