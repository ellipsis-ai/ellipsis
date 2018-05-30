import * as React from 'react';
import autobind from "../lib/autobind";

export interface DynamicLabelButtonLabel {
  text: string,
  mobileText?: Option<string>,
  displayWhen: boolean
}

interface Props {
  type?: Option<string>,
  className?: Option<string>,
  disabledWhen?: Option<boolean>,
  onClick: () => void,
  labels: Array<DynamicLabelButtonLabel>
}

interface State {
  minWidth: Option<number>
}

class DynamicLabelButton extends React.PureComponent<Props, State> {
    buttonLabels: Array<Option<HTMLDivElement>>;
    button: Option<HTMLButtonElement>;

    constructor(props: Props) {
      super(props);
      this.buttonLabels = [];
      this.state = {
        minWidth: null
      };
      autobind(this);
    }

    adjustMinWidthIfNecessary(): void {
      var newMax = this.getMaxLabelWidth();
      if (newMax !== this.state.minWidth && typeof newMax === "number" && newMax > 0) {
        this.setState({ minWidth: newMax });
      }
    }

    componentDidMount(): void {
      this.adjustMinWidthIfNecessary();
    }

    componentDidUpdate(): void {
      this.adjustMinWidthIfNecessary();
    }

    getMaxLabelWidth(): Option<number> {
      var maxWidth = 0;
      this.buttonLabels.forEach((label) => {
        if (label && label.scrollWidth) {
          maxWidth = Math.max(maxWidth, label.scrollWidth);
        }
      });
      return maxWidth > 0 ? maxWidth : null;
    }

    getPlaceholderStyle(): React.CSSProperties | undefined {
      return this.state.minWidth ? ({
        minWidth: `${this.state.minWidth}px`
      }) : undefined;
    }

    getLabels() {
      this.buttonLabels = [];
      return this.props.labels.map((label, index) => (
        <div ref={(element) => this.buttonLabels.push(element)} key={`buttonLabel${index}`} className={
          `button-label-absolute visibility ${label.displayWhen ? "visibility-visible" : "visibility-hidden"}`
        }>
          {label.mobileText ? (
            <span>
              <span className="mobile-display-none">{label.text}</span>
              <span className="mobile-display-only">{label.mobileText}</span>
            </span>
          ) : (
            <span>{label.text}</span>
          )}
        </div>
      ));
    }

    focus() {
      if (this.button) {
        this.button.focus();
      }
    }

    onClick() {
      if (this.props.onClick) {
        this.props.onClick();
      }
    }

    render() {
      return (
        <button
          ref={(el) => this.button = el}
          type={this.props.type || "button"}
          className={this.props.className || ""}
          disabled={this.props.disabledWhen || false}
          onClick={this.onClick}
        >
          <div className="button-labels">
            <div style={this.getPlaceholderStyle()}>&nbsp;</div>
            {this.getLabels()}
          </div>
        </button>
      );
    }
}

export default DynamicLabelButton;
