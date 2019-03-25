import * as React from 'react';
import Collapsible from './collapsible';
import autobind from "../lib/autobind";

interface Props {
  name: string
  content: React.ReactNode
  onDismiss: (name: string) => void
  isDismissed?: Option<boolean>
}

class PageNotification extends React.Component<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    dismiss(): void {
      this.props.onDismiss(this.props.name);
    }

    render() {
      return (
        <div>
          <Collapsible revealWhen={!this.props.isDismissed} animateInitialRender={true}>
            <div className="bg-pink-medium pvm type-white">
              <div className="container container-c">
                <div className="mhl">
                  <span className="mrm">
                    {this.props.content}
                  </span>
                  <button type="button"
                    className="button-s button-shrink button-inherit"
                    onClick={this.dismiss}
                  >
                    OK
                  </button>
                </div>
              </div>
            </div>
          </Collapsible>
        </div>
      );
    }
}

export default PageNotification;
