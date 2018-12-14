import * as React from 'react';
import autobind from "../lib/autobind";

interface Props {
  onToggle: () => void
  expandedWhen: boolean
  children: React.ReactNode
  className?: Option<string>
}

class ExpandButton extends React.Component<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    toggle(): void {
      this.props.onToggle();
    }

    render() {
      return (
        <button type="button" className={`button-raw ${this.props.className || ""}`} onClick={this.toggle}>
          <span>
            <span className="display-inline-block mrs" style={{ width: '0.8em' }}>
              {this.props.expandedWhen ? "▾" : "▸"}
            </span>
            <span>{this.props.children}</span>
          </span>
        </button>
      );
    }
}

export default ExpandButton;
