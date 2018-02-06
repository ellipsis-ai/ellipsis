// @flow
import * as React from 'react';
import Button from '../../form/button';
import Collapsible from '../../shared_ui/collapsible';
import SVGExpand from '../../svg/expand';
import autobind from '../../lib/autobind';

type Props = {
  label: React.Node,
  children?: React.Node,
  className?: ?string
};

type State = {
  expanded: boolean
}

class DiffItem extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      autobind(this);
      this.state = {
        expanded: false
      };
    }

    hasChildren(): boolean {
      return React.Children.count(this.props.children) > 0;
    }

    toggleExpanded(): void {
      this.setState({
        expanded: !this.state.expanded
      });
    }

    renderChildren(): React.Node {
      if (this.hasChildren()) {
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
            {this.hasChildren() ? (
              <Button className="button-block width-full" onClick={this.toggleExpanded}>
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

export default DiffItem;
