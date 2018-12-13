import * as React from 'react';
import autobind from "../lib/autobind";
import {CSSProperties} from "react";
import {ReactElement} from "react";

const DEFAULT_DURATION = 0.25;

interface Props {
  animationDisabled?: boolean,
  animationDuration?: Option<number>,
  children: any,
  className?: Option<string>,
  revealWhen: boolean,
  animateInitialRender?: boolean,
  isHorizontal?: boolean,
  onChange?: (isRevealed: boolean) => void
}

interface State {
  isAnimating: boolean,
  isRevealed: boolean
}

interface ContainingElement {
  "data-is-animating": boolean,
  "data-is-revealed": boolean
}

class Collapsible extends React.Component<Props, State> {
/*
The Collapsible component reveals or collapses its children in the DOM in response
to the boolean value of its revealWhen property, using the max-height CSS property.

Animation speed defaults to 0.25s, or can be set with the animationDuration property,
which should be set with a number (not a string).

Note: to allow for child content to be dynamic in height/width and to overflow the
bounds, max-height/width and overflow get cleared after reveal, and reset before collapse.
*/
  timers: Array<number>;
  container: Option<HTMLDivElement>;

  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = {
      isAnimating: false,
      isRevealed: this.props.revealWhen
    };
    this.timers = [];
    this.container = null;
  }

  animationDisabled(): boolean {
    const container = this.container;
    return Boolean(this.props.animationDisabled || !container ||
      (container && container.parentElement && !container.parentElement.offsetHeight && !container.parentElement.offsetWidth));
  }

  setContainerStyle(name: "transition" | "maxHeight" | "maxWidth" | "overflow" | "display", value: any): void {
    if (this.container) {
      this.container.style[name] = value;
    }
  }

  isVertical(): boolean {
    return !this.props.isHorizontal;
  }

  animationDurationSeconds(): number {
    return this.props.animationDuration || DEFAULT_DURATION;
  }

  animationDurationMilliseconds(): number {
    return this.animationDurationSeconds() * 1000;
  }

  after(callback: () => void): void {
    if (this.animationDisabled()) {
      callback();
    } else {
      this.timers.push(setTimeout(() => {
        callback();
      }, 1));
    }
  }

  afterAnimation(callback: () => void): void {
    var f = () => {
      callback();
      if (this.props.onChange) {
        this.props.onChange(this.props.revealWhen);
      }
    };
    if (this.animationDisabled()) {
      f();
    } else {
      this.timers.push(setTimeout(f, this.animationDurationMilliseconds()));
    }
  }

  addTransition(): void {
    var propName = this.isVertical() ? 'max-height' : 'max-width';
    this.setContainerStyle("transition", this.animationDisabled() ?
      null : `${propName} ${this.animationDurationSeconds()}s ease`);
  }

  removeTransition(): void {
    this.setContainerStyle("transition", null);
  }

  setMaxHeight(height: string): void {
    this.setContainerStyle("maxHeight", height);
  }

  setMaxWidth(width: string): void {
    this.setContainerStyle("maxWidth", width);
  }

  setOverflow(overflow: string): void {
    this.setContainerStyle("overflow", overflow);
  }

  setCurrentHeight(): void {
    var c = this.container;
    this.setMaxHeight((c ? c.scrollHeight : 0) + 'px');
  }

  setNoHeight(): void {
    this.setMaxHeight('0px');
  }

  setAutoHeight(): void {
    this.setMaxHeight('none');
  }

  setCurrentWidth(): void {
    var c = this.container;
    this.setMaxWidth((c ? c.scrollWidth : 0) + 'px');
  }

  setNoWidth(): void {
    this.setMaxWidth('0px');
  }

  setAutoWidth(): void {
    this.setMaxWidth('none');
  }

  setHidden(): void {
    this.setContainerStyle("display", 'none');
  }

  setVisible(): void {
    this.setContainerStyle("display", null);
  }

  collapse(): void {
    this.setState({
      isAnimating: !this.animationDisabled()
    }, () => {
      this.removeTransition();
      if (this.isVertical()) {
        this.setCurrentHeight();
      } else {
        this.setCurrentWidth();
      }
      this.setOverflow('hidden');
      this.after(this.doCollapse);
    });
  }

  doCollapse(): void {
    this.addTransition();
    if (this.isVertical()) {
      this.setNoHeight();
    } else {
      this.setNoWidth();
    }
    this.setState({
      isRevealed: false
    }, () => {
      this.afterAnimation(this.finishCollapse);
    });
  }

  finishCollapse(): void {
    this.setHidden();
    this.setState({
      isAnimating: false
    });
  }

  reveal(): void {
    this.setState({
      isAnimating: !this.animationDisabled()
    }, () => {
      this.setVisible();
      this.addTransition();
      if (this.isVertical()) {
        this.setCurrentHeight();
      } else {
        this.setCurrentWidth();
      }
      this.setState({
        isRevealed: true
      }, () => {
        this.afterAnimation(this.afterReveal);
      });
    });
  }

  afterReveal(): void {
    this.removeTransition();
    if (this.isVertical()) {
      this.setAutoHeight();
    } else {
      this.setAutoWidth();
    }
    this.setOverflow('visible');
    this.setState({
      isAnimating: false
    });
  }

  componentDidMount(): void {
    if (this.props.animateInitialRender && this.props.revealWhen) {
      this.reveal();
    } else if (this.props.revealWhen) {
      this.afterReveal();
    } else {
      this.finishCollapse();
    }
    this.addTransition();
  }

  componentDidUpdate(prevProps: Props): void {
    if (prevProps.revealWhen === this.props.revealWhen) {
      return;
    }
    if (this.state.isAnimating) {
      this.timers.push(setTimeout(() => {
        this.componentDidUpdate(prevProps);
      }, this.animationDurationMilliseconds()));
    } else if (this.props.revealWhen) {
      this.reveal();
    } else {
      this.collapse();
    }
  }

  componentWillUnmount(): void {
    this.timers.forEach((timer) => clearTimeout(timer));
  }

  getDefaultStyle(): CSSProperties {
    const styles: CSSProperties = {
      overflow: "hidden"
    };
    styles[this.isVertical() ? 'maxHeight' : 'maxWidth'] = '0px';
    return styles;
  }

  elementIsValidElement(el: any): el is ReactElement<ContainingElement> {
    return el && React.isValidElement(el);
  }

  render() {
    return (
      <div ref={(el) => this.container = el} style={this.getDefaultStyle()} className={this.props.className || ""}>
        {React.Children.map(this.props.children, (ea) => {
          // Force children to re-render when animation starts or stops by inserting an extra data attribute
          if (this.elementIsValidElement(ea)) {
            return React.cloneElement(ea, {
              "data-is-animating": this.state.isAnimating,
              "data-is-revealed": this.state.isRevealed
            });
          } else {
            return ea;
          }
        })}
      </div>
    );
  }
}

export default Collapsible;
