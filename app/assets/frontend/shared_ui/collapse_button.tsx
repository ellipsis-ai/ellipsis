import * as React from 'react';
import SVGCollapse from '../svg/collapse';
import autobind from "../lib/autobind";

interface Props {
  direction?: Option<string>
  onClick: () => void
}

class CollapseButton extends React.PureComponent<Props> {
    constructor(props) {
      super(props);
      autobind(this);
    }

    onClick() {
      this.props.onClick();
    }

    render() {
      return (
        <button type="button" className="button-raw type-weak align-t" onClick={this.onClick} style={{ height: "22px" }}>
          <SVGCollapse direction={this.props.direction} />
        </button>
      );
    }
}

export default CollapseButton;
