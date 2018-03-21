import * as React from 'react';
import Button from '../form/button';
import SVGQuestionMark from '../svg/question_mark';
import SVGXIcon from '../svg/x';
import autobind from '../lib/autobind';

type Props = {
  className?: string,
  inline?: boolean,
  onClick: () => void,
  toggled?: boolean
}

class HelpButton extends React.PureComponent<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    onClick(): void {
      this.props.onClick();
    }

    render() {
      return (
        <span className="position-relative">
          <Button
            className={
              `button-symbol button-s ${
                this.props.toggled ? "button-help-toggled" : ""
              } ${
                this.props.inline ? "button-subtle" : ""
              } ${this.props.className || ""}`
            }
            onClick={this.onClick}
          >
            {this.props.toggled ? (<SVGXIcon label="Close" />) : (<SVGQuestionMark />)}
          </Button>
        </span>
      );
    }
}

export default HelpButton;
