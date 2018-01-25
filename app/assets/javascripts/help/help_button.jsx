// @flow
define(function(require) {
  const React = require('react'),
    Button = require('../form/button'),
    SVGQuestionMark = require('../svg/question_mark'),
    SVGXIcon = require('../svg/x'),
    autobind = require('../lib/autobind');

  type Props = {
    className?: string,
    inline?: boolean,
    onClick: () => void,
    toggled?: boolean
  }

  class HelpButton extends React.PureComponent<Props> {
    props: Props;

    constructor(props) {
      super(props);
      autobind(this);
    }

    onClick(): void {
      this.props.onClick();
    }

    render(): React.Node {
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

  return HelpButton;

});
