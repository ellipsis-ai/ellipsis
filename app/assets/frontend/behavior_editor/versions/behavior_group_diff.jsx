// @flow
import {Diff, AddedOrRemovedDiff, ModifiedDiff, MultiLineTextPropertyDiff, OrderingDiff} from '../../models/diffs';
import * as React from 'react';
import DiffItem from './diff_item';
import TextDiff from './text_diff';
import OrderingDiffDetail from './ordering_diff_detail';
import BehaviorGroup from '../../models/behavior_group';
import autobind from '../../lib/autobind';

type Props = {
  diff: ModifiedDiff<BehaviorGroup>
};

type AddedRemovedModifiedDiff = AddedOrRemovedDiff<*> | ModifiedDiff<*>;

class BehaviorGroupDiff extends React.Component<Props> {
    constructor(props: Props): void {
      super(props);
      autobind(this);
    }

    renderTextDiff(diff: MultiLineTextPropertyDiff, index: number, className: ?string) {
      return (
        <DiffItem className={className} key={`diff${index}`} label={diff.summaryText()}>
          <TextDiff diff={diff} className="bg-white mbneg1" />
        </DiffItem>
      );
    }

    renderOrderingDiff(diff: OrderingDiff<*>, index: number, className: ?string) {
      return (
        <DiffItem className={className} key={`diff${index}`} label={diff.summaryText()}>
          <OrderingDiffDetail diff={diff} className={className} />
        </DiffItem>
      );
    }

    renderAddedRemovedModifiedDiff(diff: AddedRemovedModifiedDiff, index: number, className: ?string) {
      return (
        <DiffItem className={className} key={`diff${index}`} label={diff.summaryText()}>
          {this.renderDiffChildren(diff)}
        </DiffItem>
      );
    }

    renderSingleDiff(diff: Diff, index: number, childClassName: ?string): React.Node {
      if (diff instanceof MultiLineTextPropertyDiff) {
        return this.renderTextDiff(diff, index, childClassName);
      } else if (diff instanceof OrderingDiff) {
        return this.renderOrderingDiff(diff, index, childClassName);
      } else if (diff instanceof AddedOrRemovedDiff || diff instanceof ModifiedDiff) {
        return this.renderAddedRemovedModifiedDiff(diff, index, childClassName);
      }
    }

    renderDiffChildren(diff: Diff, childClassName: ?string): React.Node {
      if (diff instanceof ModifiedDiff || diff instanceof AddedOrRemovedDiff) {
        return diff.children.map((ea, index) => this.renderSingleDiff(ea, index, childClassName));
      }
    }

    render(): React.Node {
      return (
        <div>
          {this.renderDiffChildren(this.props.diff, "pal border mbneg1 bg-white")}
        </div>
      );
    }
}

export default BehaviorGroupDiff;
