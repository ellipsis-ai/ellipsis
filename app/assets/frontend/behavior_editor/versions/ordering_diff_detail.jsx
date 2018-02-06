// @flow
import * as React from 'react';
import {OrderingDiff} from '../../models/diffs';

type Props = {
  className: ?string,
  diff: OrderingDiff<*>
};

class OrderingDiffDetail extends React.PureComponent<Props> {
    props: Props;

    orderedList(items: Array<*>) {
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
          <div className="columns columns-elastic">
            <div className="column-group">
              <div className="column-row">
                <div className="column column-shrink bg-pink-light pvxs phs align-r"><span className="type-label">Before</span></div>
                <div className="column column-expand bg-pink-lightest pvxs phs">{this.getFromOrder()}</div>
              </div>
              <div className="column-row">
                <div className="column column-shrink bg-green-light pvxs phs align-r"><span className="type-label">After</span></div>
                <div className="column column-expand bg-green-lightest pvxs phs">{this.getToOrder()}</div>
              </div>
            </div>
          </div>
        </div>
      );
    }
}

export default OrderingDiffDetail;
