// @flow
define(function(require) {
  const React = require('react'),
    autobind = require('../lib/autobind');

  type Props = {
    children: React.Node,
    className: ?string,
    onHeightChange: (number) => void
  }

  type State = {
    exceedsWindowHeight: boolean
  }

  class FixedHeader extends React.Component<Props, State> {
    intervalId: ?number;
    header: ?HTMLDivElement;
    currentScrollHeight: number;
    props: Props;
    state: State;

    constructor(props) {
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

    render(): React.Node {
      return (
        <header
          ref={(el) => this.header = el}
          className={`position-fixed-top position-z-front ${this.props.className || ""}`}
          style={(this.state.exceedsWindowHeight ? { overflowY: 'auto' } : {})}
        >
          {this.props.children}
        </header>
      );
    }
  }

  return FixedHeader;
});
