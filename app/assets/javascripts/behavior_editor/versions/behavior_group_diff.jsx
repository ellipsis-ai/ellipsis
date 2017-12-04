// @flow

import type {Diff} from '../../models/diffs';
import type {ElementType} from 'react';

define(function(require) {
  const React = require('react'),
    diffs = require('../../models/diffs'),
    DiffItem = require('./diff_item'),
    TextDiff = require('./text_diff'),
    BehaviorGroup = require('../../models/behavior_group'),
    Editable = require('../../models/editable'),
    autobind = require('../../lib/autobind');

  type Props = {
    diff: diffs.ModifiedDiff<BehaviorGroup>
  };

  class BehaviorGroupDiff extends React.Component<Props> {
    props: Props;

    constructor(props): void {
      super(props);
      autobind(this);
    }

    getSkillTextDiffs(): Array<diffs.TextPropertyDiff> {
      return this.props.diff.children.filter((ea) => ea instanceof diffs.TextPropertyDiff);
    }

    getActionDiffs(): Array<Diff> {
      return this.props.diff.children.filter((ea) => {
        const item = ea.item;
        return item && item instanceof Editable && item.isBehaviorVersion() && !item.isDataType();
      });
    }

    getDataTypeDiffs(): Array<Diff> {
      return this.props.diff.children.filter((ea) => {
        const item = ea.item;
        return item && item instanceof Editable && item.isBehaviorVersion() && item.isDataType();
      });
    }

    getLibraryDiffs(): Array<Diff> {
      return this.props.diff.children.filter((ea) => {
        const item = ea.item;
        return item && item instanceof Editable && item.isLibraryVersion();
      });
    }

    renderEditableDiff(diff: Diff, index: number, keyPrefix: string): ElementType {
      return (
        <DiffItem key={`${keyPrefix}${index}`} label={this.getLabelForEditableDiff(diff)}>
          {this.renderEditableDiffDetails(diff)}
        </DiffItem>
      );
    }

    getLabelForEditableDiff(diff: Diff): React.Node {
      const summary = diff.summaryText();
      if (diff instanceof diffs.AddedOrRemovedDiff) {
        return (
          <span>{summary}: <b>{diff.item.getName()}</b></span>
        );
      } else if (diff instanceof diffs.ModifiedDiff) {
        const newName = diff.modified.getName();
        const originalName = diff.original.getName();
        return (
          <span>
            <span>{summary}: </span>
            <b>
              <span>{originalName} </span>
              {originalName === newName ? null : (
                <span>→ {newName}</span>
              )}
            </b>
          </span>
        );
      }
    }

    renderEditableDiffDetails(diff: Diff): React.Node {
      return null;
    }

    render(): React.Node {
      return (
        <div>
          {this.getSkillTextDiffs().map((textDiff, index) => (
            <DiffItem key={`skillDiff${index}`} label={`${textDiff.label} changed:`}>
              <TextDiff parts={textDiff.parts} />
            </DiffItem>
          ))}
          {this.getActionDiffs().map((diff, index) => this.renderEditableDiff(diff, index, 'action'))}
          {this.getDataTypeDiffs().map((diff, index) => this.renderEditableDiff(diff, index, 'dataType'))}
          {this.getLibraryDiffs().map((diff, index) => this.renderEditableDiff(diff, index, 'library'))}
        </div>
      );
    }
  }

  return BehaviorGroupDiff;
});
