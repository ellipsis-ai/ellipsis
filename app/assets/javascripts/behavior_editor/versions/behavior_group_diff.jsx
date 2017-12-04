// @flow
define(function(require) {
  const React = require('react'),
    diffs = require('../../models/diffs'),
    TextDiff = require('./text_diff'),
    BehaviorGroup = require('../../models/behavior_group'),
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

    render(): React.Node {
      return (
        <div>
          {this.getSkillTextDiffs().map((textDiff, index) => (
            <div className="mbl" key={`skillDiff${index}`}>
              <div className="type-italic type-weak mbxs">{textDiff.label} changed:</div>
              <div className="border pas bg-white type-s">
                <TextDiff parts={textDiff.parts} />
              </div>
            </div>
          ))}
        </div>
      );
    }
  }

  return BehaviorGroupDiff;
});
