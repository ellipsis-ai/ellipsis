import * as React from 'react';
import SVGXIcon from '../svg/x';
import autobind from "../lib/autobind";
import Button from "../form/button";

interface Props {
  onClick: () => void
  title?: Option<string>
  className?: Option<string>
}

class DeleteButton extends React.PureComponent<Props> {
  button: Option<Button>;

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
      <Button
        ref={(el) => this.button = el}
        className={`button-subtle button-symbol ${this.props.className || ""}`}
        onClick={this.onClick}
        title={this.props.title || "Delete"}
      >
        <span className="type-weak"><SVGXIcon label="Delete" /></span>
      </Button>
    );
  }
}

export default DeleteButton;
