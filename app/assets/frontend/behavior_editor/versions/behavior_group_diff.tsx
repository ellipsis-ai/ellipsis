import {
  Diff, AddedOrRemovedDiff, ModifiedDiff, MultiLineTextPropertyDiff, OrderingDiff, Diffable,
  BooleanPropertyDiff, CategoricalPropertyDiff
} from '../../models/diffs';
import * as React from 'react';
import DiffItem from './diff_item';
import TextDiff from './text_diff';
import OrderingDiffDetail from './ordering_diff_detail';
import BehaviorGroup from '../../models/behavior_group';
import autobind from '../../lib/autobind';

type Props = {
  diff: ModifiedDiff<BehaviorGroup>
};

class BehaviorGroupDiff extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  renderTextDiff(diff: MultiLineTextPropertyDiff, index: number, className?: Option<string>) {
    return (
      <DiffItem className={className} key={`diff${index}`} label={diff.summaryText()}>
        <TextDiff diff={diff} className="bg-white mbneg1" />
      </DiffItem>
    );
  }

  renderOrderingDiff(diff: OrderingDiff<Diffable>, index: number, className?: Option<string>) {
    return (
      <DiffItem className={className} key={`diff${index}`} label={diff.summaryText()}>
        <OrderingDiffDetail diff={diff} className={className} />
      </DiffItem>
    );
  }

  renderSimpleDiff(diff: BooleanPropertyDiff | CategoricalPropertyDiff, index: number, className?: Option<string>) {
    return (
      <DiffItem className={className} key={`diff${index}`} label={diff.summaryText()} />
    )
  }

  renderAddedRemovedModifiedDiff(diff: AddedOrRemovedDiff<Diffable> | ModifiedDiff<Diffable>, index: number, className?: Option<string>) {
    return (
      <DiffItem className={className} key={`diff${index}`} label={diff.summaryText()}>
        {this.renderDiffChildren(diff)}
      </DiffItem>
    );
  }

  renderSingleDiff(diff: Diff, index: number, childClassName?: Option<string>) {
    if (diff instanceof MultiLineTextPropertyDiff) {
      return this.renderTextDiff(diff, index, childClassName);
    } else if (diff instanceof OrderingDiff) {
      return this.renderOrderingDiff(diff, index, childClassName);
    } else if (diff instanceof BooleanPropertyDiff || diff instanceof CategoricalPropertyDiff) {
      return this.renderSimpleDiff(diff, index, childClassName);
    } else if (diff instanceof AddedOrRemovedDiff || diff instanceof ModifiedDiff) {
      return this.renderAddedRemovedModifiedDiff(diff, index, childClassName);
    } else {
      return undefined;
    }
  }

  renderDiffChildren(diff: Diff, childClassName?: Option<string>) {
    if (diff instanceof ModifiedDiff || diff instanceof AddedOrRemovedDiff) {
      return diff.children.map((ea, index) => this.renderSingleDiff(ea, index, childClassName));
    } else {
      return undefined;
    }
  }

  render() {
    return (
      <div>
        {this.renderDiffChildren(this.props.diff, "pal border mbneg1 bg-white")}
      </div>
    );
  }
}

export default BehaviorGroupDiff;
