// @flow
define(function(require) {
  const React = require('react'),
    BehaviorGroup = require('../models/behavior_group'),
    Formatter = require('../lib/formatter'),
    autobind = require('../lib/autobind');

  type Props = {
    group: BehaviorGroup,
    currentUserId: string,
    isLastSavedVersion: ?boolean,
    className: ?string
  };

  class BehaviorGroupSaveInfo extends React.PureComponent<Props> {
    props: Props;

    constructor(props) {
      super(props);
      autobind(this);
    }

    prefix(): string {
      if (this.props.isLastSavedVersion) {
        return this.savedByCurrentUser() ? "You last saved this skill" : "Skill last saved";
      } else {
        return this.savedByCurrentUser() ? "You saved this version" : "Version saved";
      }
    }

    savedByCurrentUser(): boolean {
      return this.props.group.author && this.props.group.author.id === this.props.currentUserId;
    }

    render(): React.Node {
      const group = this.props.group;
      const authorName = group.author && group.author.userName ? group.author.formattedFullNameOrUserName() : null;
      return (
        <span className={this.props.className || ""}>
          <span>{this.prefix()} </span>
          <span>{Formatter.formatTimestampRelativeIfRecent(group.createdAt)}</span>
          <span> {!this.savedByCurrentUser() && authorName ? `by ${authorName}` : ""}</span>
        </span>
      );
    }
  }

  return BehaviorGroupSaveInfo;
});
