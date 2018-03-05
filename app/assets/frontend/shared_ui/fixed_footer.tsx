import * as React from 'react';
import autobind from '../lib/autobind';

type Props = {
  children: any,
  className?: string,
  onHeightChange: (number) => void
}

type State = {
  exceedsWindowHeight: boolean
}

class FixedFooter extends React.Component<Props, State> {
    intervalId: number | null;
    footer: HTMLElement | null;
    currentScrollHeight: number;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.intervalId = null;
      this.footer = null;
      this.currentScrollHeight = 0;
      this.state = {
        exceedsWindowHeight: false
      };
    }

    checkHeight(): void {
      var footerScrollHeight = this.footer ? this.footer.scrollHeight : 0;
      var windowHeight = window.innerHeight;
      var exceedsWindowHeight = footerScrollHeight > windowHeight;
      if (this.state.exceedsWindowHeight !== exceedsWindowHeight) {
        this.setState({ exceedsWindowHeight: exceedsWindowHeight });
      }
      if (footerScrollHeight !== this.currentScrollHeight) {
        this.currentScrollHeight = footerScrollHeight;
        if (this.props.onHeightChange) {
          this.props.onHeightChange(this.currentScrollHeight);
        }
      }
    }

    componentDidMount(): void {
      this.intervalId = setInterval(this.checkHeight, 150);
    }

    componentWillUnmount(): void {
      if (this.intervalId) {
        clearInterval(this.intervalId);
      }
    }

    render() {
      return (
        <footer
          ref={(el) => this.footer = el}
          className={`position-fixed-bottom position-z-front ${this.props.className || ""}`}
          style={(this.state.exceedsWindowHeight ? { overflowY: 'auto' } : {})}
        >
          {this.props.children}
        </footer>
      );
    }
}

export default FixedFooter;

