// @flow
define(function(require) {
  const React = require('react');

  type Props = {
    label: React.Node,
    children: Array<React.Node>,
    className?: string
  };

  class DiffItem extends React.PureComponent<Props> {
    props: Props;

    render(): React.Node {
      return (
        <div className={this.props.className || "pas border-bottom mbneg1"}>
          <div>{this.props.label}</div>
          {React.Children.map(this.props.children, (child) => (
            <div className="mtxs border bg-lightest type-s">{child}</div>
          ))}
        </div>
      );
    }
  }

  return DiffItem;
});
