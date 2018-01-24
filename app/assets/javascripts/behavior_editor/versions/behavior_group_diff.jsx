// @flow

import type {Diff} from '../../models/diffs';
import type {ElementType} from 'react';

define(function(require) {
  const React = require('react'),
    diffs = require('../../models/diffs'),
    DiffItem = require('./diff_item'),
    TextDiff = require('./text_diff'),
    OrderingDiffDetail = require('./ordering_diff_detail'),
    BehaviorGroup = require('../../models/behavior_group'),
    autobind = require('../../lib/autobind'),
    AddedOrRemovedDiff = diffs.AddedOrRemovedDiff,
    ModifiedDiff = diffs.ModifiedDiff,
    MultiLineTextPropertyDiff = diffs.MultiLineTextPropertyDiff,
    OrderingDiff = diffs.OrderingDiff;

  type Props = {
    diff: ModifiedDiff<BehaviorGroup>
  };

  type AddedRemovedModifiedDiff = AddedOrRemovedDiff | ModifiedDiff;

  class BehaviorGroupDiff extends React.Component<Props> {
    props: Props;

    constructor(props): void {
      super(props);
      autobind(this);
    }

    renderTextDiff(diff: MultiLineTextPropertyDiff, index: number, className: ?string): ElementType {
      return (
        <DiffItem className={className} key={`diff${index}`} label={diff.summaryText()}>
          <TextDiff diff={diff} className="bg-white mbneg1" />
        </DiffItem>
      );
    }

    renderOrderingDiff(diff: OrderingDiff, index: number, className: ?string): ElementType {
      return (
        <DiffItem className={className} key={`diff${index}`} label={diff.summaryText()}>
          <OrderingDiffDetail diff={diff} className={className} />
        </DiffItem>
      );
    }

    renderAddedRemovedModifiedDiff(diff: AddedRemovedModifiedDiff, index: number, className: ?string): ElementType {
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
      } else {
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

  return BehaviorGroupDiff;
});
