// @flow

import type {Diff} from '../../models/diffs';
import type {ElementType} from 'react';

define(function(require) {
  const React = require('react'),
    diffs = require('../../models/diffs'),
    DiffItem = require('./diff_item'),
    TextDiff = require('./text_diff'),
    BehaviorGroup = require('../../models/behavior_group'),
    autobind = require('../../lib/autobind'),
    AddedOrRemovedDiff = diffs.AddedOrRemovedDiff,
    ModifiedDiff = diffs.ModifiedDiff,
    TextPropertyDiff = diffs.TextPropertyDiff;

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

    renderTextDiff(diff: TextPropertyDiff, index: number, className: ?string): ElementType {
      return (
        <DiffItem className={className} key={`diff${index}`} label={`${diff.label} changed:`}>
          <TextDiff parts={diff.parts} isCode={diff.isCode} className="bg-white mbneg1" />
        </DiffItem>
      );
    }

    renderAddedRemovedModifiedDiff(diff: AddedRemovedModifiedDiff, index: number, className: ?string): ElementType {
      return (
        <DiffItem className={className} key={`diff${index}`} label={this.getLabelForDiff(diff)}>
          {this.renderDiffDetails(diff)}
        </DiffItem>
      );
    }

    getLabelForDiff(diff: AddedRemovedModifiedDiff): React.Node {
      return diff.summaryText();
    }

    renderDiffDetails(diff: Diff, childClassName: ?string): React.Node {
      if (diff instanceof ModifiedDiff) {
        const children = diff.children;
        return (
          <div>
            {children.map((ea, index) => {
              if (ea instanceof TextPropertyDiff) {
                return this.renderTextDiff(ea, index, childClassName);
              } else {
                return this.renderAddedRemovedModifiedDiff(ea, index, childClassName);
              }
            })}
          </div>
        );
      }
    }

    render(): React.Node {
      return (
        <div>
          {this.renderDiffDetails(this.props.diff, "pal border mbneg1 bg-white")}
        </div>
      );
    }
  }

  return BehaviorGroupDiff;
});
