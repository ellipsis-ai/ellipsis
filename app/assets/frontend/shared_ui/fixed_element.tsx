import * as React from 'react';
import autobind from '../lib/autobind';
import {CSSProperties} from "react";

export interface FixedElementProps {
  children: any;
  className?: string;
  zIndexClassName?: string;
  onHeightChange: (number) => void;
  marginTop?: Option<number>;
}

type Props = FixedElementProps & {
  elementType: "header" | "footer"
}

type State = {
  exceedsWindowHeight: boolean
}

class FixedElement extends React.Component<Props, State> {
    intervalId: Option<number>;
    element: Option<HTMLElement>;
    currentScrollHeight: number;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.intervalId = null;
      this.element = null;
      this.currentScrollHeight = 0;
      this.state = {
        exceedsWindowHeight: false
      };
    }

    checkHeight(): void {
      var elementScrollHeight = this.element ? this.element.scrollHeight : 0;
      var windowHeight = window.innerHeight;
      var exceedsWindowHeight = elementScrollHeight > windowHeight;
      if (this.state.exceedsWindowHeight !== exceedsWindowHeight) {
        this.setState({ exceedsWindowHeight: exceedsWindowHeight });
      }
      if (elementScrollHeight !== this.currentScrollHeight) {
        this.currentScrollHeight = elementScrollHeight;
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
      return React.createElement(this.props.elementType, {
        ref: (el) => this.element = el,
        className: `position-fixed-${this.props.elementType === "header" ? "top": "bottom"} ${this.props.zIndexClassName || "position-z-front"} ${this.props.className || ""}`,
        style: this.getStyle()
      }, this.props.children);
    }
}

export default FixedElement;
