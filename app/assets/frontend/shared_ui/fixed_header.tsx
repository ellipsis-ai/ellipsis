import * as React from 'react';
import autobind from '../lib/autobind';
import {CSSProperties} from "react";

type Props = {
  children: any,
  className?: string,
  onHeightChange: (number) => void,
  marginTop?: Option<number>
}

type State = {
  exceedsWindowHeight: boolean
}

class FixedHeader extends React.Component<Props, State> {
    intervalId: Option<number>;
    header: Option<HTMLElement>;
    currentScrollHeight: number;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.intervalId = null;
      this.header = null;
      this.currentScrollHeight = 0;
      this.state = {
        exceedsWindowHeight: false
      };
    }

    checkHeight(): void {
      var headerScrollHeight = this.header ? this.header.scrollHeight : 0;
      var windowHeight = window.innerHeight;
      var exceedsWindowHeight = headerScrollHeight > windowHeight;
      if (this.state.exceedsWindowHeight !== exceedsWindowHeight) {
        this.setState({ exceedsWindowHeight: exceedsWindowHeight });
      }
      if (headerScrollHeight !== this.currentScrollHeight) {
        this.currentScrollHeight = headerScrollHeight;
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

    getStyle(): CSSProperties {
      const style: CSSProperties = {
        marginTop: `${this.props.marginTop || 0}px`
      };
      if (this.state.exceedsWindowHeight) {
        style.overflowY = 'auto';
      }
      return style;
    }

    render() {
      return (
        <header
          ref={(el) => this.header = el}
          className={`position-fixed-top position-z-front ${this.props.className || ""}`}
          style={this.getStyle()}
        >
          {this.props.children}
        </header>
      );
    }
}

export default FixedHeader;
