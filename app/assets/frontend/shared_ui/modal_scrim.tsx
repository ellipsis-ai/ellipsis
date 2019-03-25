import * as React from 'react';
import autobind from "../lib/autobind";

interface Props {
  isActive: boolean
  onClick?: () => void
}

class ModalScrim extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  onClick(): void {
    if (this.props.onClick) {
      this.props.onClick();
    }
  }

  render() {
    return (
      <div
        className={
          "bg-scrim position-z-scrim position-fixed-full " +
          (this.props.isActive ? "fade-in" : "display-none")
        }
        onClick={this.onClick}
      />
    );
  }
}

export default ModalScrim;
