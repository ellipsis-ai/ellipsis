// @flow
define(function(require) {
  const React = require('react');

  type Props = {
    label: React.Node,
    children: Array<React.Node>
  };

  class DiffItem extends React.PureComponent<Props> {
    props: Props;

    render(): React.Node {
      return (
        <div className="mbl">
          <div className="type-italic type-weak mbxs">{this.props.label}</div>
          {React.Children.map(this.props.children, (child) => (
            <div className="border bg-white type-s mbneg1">{child}</div>
          ))}
        </div>
      );
    }
  }

  return DiffItem;
});
