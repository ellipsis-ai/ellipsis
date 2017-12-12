// @flow
define(function(require) {
  const React = require('react'),
    Button = require('../../form/button'),
    Collapsible = require('../../shared_ui/collapsible'),
    SVGExpand = require('../../svg/expand'),
    autobind = require('../../lib/autobind');

  type Props = {
    label: React.Node,
    children?: React.Node,
    className?: string
  };

  type State = {
    expanded: boolean
  }

  class DiffItem extends React.Component<Props, State> {
    props: Props;
    state: State;

    constructor(props) {
      super(props);
      autobind(this);
      this.state = {
        expanded: false
      };
    }

    toggleExpanded(): void {
      this.setState({
        expanded: !this.state.expanded
      });
    }

    renderChildren(): React.Node {
      if (this.props.children) {
        return (
          <Collapsible revealWhen={this.state.expanded}>
            {React.Children.map(this.props.children, (child) => (
              <div className="mtxs border bg-lightest type-s">{child}</div>
            ))}
          </Collapsible>
        );
      }
    }

    render(): React.Node {
      return (
        <div className={this.props.className || "pas border-bottom mbneg1"}>
          <div>
            {this.props.children ? (
              <Button className="button-block" onClick={this.toggleExpanded}>
                <span className="display-inline-block align-m height-xl mrs">
                  <SVGExpand expanded={this.state.expanded} />
                </span>
                <span className="display-inline-block align-m">
                  {this.props.label}
                </span>
              </Button>
            ) : this.props.label}
          </div>
          {this.renderChildren()}
        </div>
      );
    }
  }

  return DiffItem;
});
