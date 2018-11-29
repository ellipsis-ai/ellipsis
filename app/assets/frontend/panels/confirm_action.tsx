import * as React from 'react';
import DynamicLabelButton, {DynamicLabelButtonLabel} from '../form/dynamic_label_button';
import autobind from "../lib/autobind";
import Button from "../form/button";

interface Props {
  children: React.ReactNode
  confirmText?: Option<string>
  confirmingText?: Option<string>
  cancelText?: Option<string>
  onCancelClick: () => void
  onConfirmClick: () => void
  isConfirming?: Option<boolean>
}

class ConfirmActionPanel extends React.Component<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    getConfirmButtonLabels(): Array<DynamicLabelButtonLabel> {
      const labels: Array<DynamicLabelButtonLabel> = [{
        text: this.props.confirmText || "OK",
        displayWhen: !this.props.isConfirming
      }];
      if (this.props.confirmingText) {
        labels.push({
          text: this.props.confirmingText,
          displayWhen: Boolean(this.props.isConfirming)
        });
      }
      return labels;
    }

    onCancelClick(): void {
      this.props.onCancelClick();
    }

    render() {
      return (
        <div className="box-action">
          <div className="container phn">
            <div>
              {this.props.children}
            </div>
            <div className="mtl">
              <DynamicLabelButton className="mrs mbs"
                disabledWhen={this.props.isConfirming}
                onClick={this.props.onConfirmClick}
                labels={this.getConfirmButtonLabels()}
              />
              <Button
                className="button-primary mbs"
                onClick={this.onCancelClick}
                disabled={Boolean(this.props.isConfirming)}>
                {this.props.cancelText || "Cancel"}
              </Button>
            </div>
          </div>
        </div>
      );
    }
}

export default ConfirmActionPanel;
