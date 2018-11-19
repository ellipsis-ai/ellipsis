import * as React from 'react';
import SVGXIcon from '../svg/x';
import autobind from "../lib/autobind";

interface Props {
  onClick: () => void
  title?: Option<string>
}

class DeleteButton extends React.PureComponent<Props> {
  button: Option<HTMLButtonElement>;

  constructor(props) {
    super(props);
    autobind(this);
  }

  onClick() {
    this.props.onClick();
    if (this.button) {
      this.button.blur();
    }
  }

  render() {
    return (
      <span className="type-weak">
        <button type="button"
          ref={(el) => this.button = el}
          className="button-subtle button-symbol"
          onClick={this.onClick}
          title={this.props.title || "Delete"}
        >
          <SVGXIcon label="Delete" />
        </button>
      </span>
    );
  }
}

export default DeleteButton;
