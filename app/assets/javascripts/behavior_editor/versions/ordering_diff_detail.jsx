// @flow
define(function(require) {
  const React = require('react'),
    diffs = require('../../models/diffs');

  type Props = {
    className: ?string,
    diff: diffs.OrderingDiff
  };

  class OrderingDiffDetail extends React.PureComponent<Props> {
    props: Props;

    orderedList(items) {
      return items.map((ea, index) => {
        const number = index + 1;
        const label = ea.itemLabel() || ea.diffLabel();
        return (
          <span key={`item${index}`} className="mrs">
            <span className={`mhs bg-almost-black type-white phxs border-radius min-width-border-radius display-inline-block align-c type-xs`}>{number}</span>
            <span className="type-monospace">{label}</span>
          </span>
        );
      });
    }

    getFromOrder() {
      return this.orderedList(this.props.diff.beforeItems);
    }

    getToOrder() {
      return this.orderedList(this.props.diff.afterItems);
    }

    render(): React.Node {
      return (
        <div className={this.props.className || ""}>
          <div className="bg-pink-lightest pvxs phs">
            <span>from</span>
            {this.getFromOrder()}
          </div>
          <div className="bg-green-lightest pvxs phs">
            <span>to</span>
            {this.getToOrder()}
          </div>
        </div>
      );
    }
  }

  return OrderingDiffDetail;
});
