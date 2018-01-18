// @flow
define(function(require) {
  const React = require('react'),
    BehaviorGroup = require('../models/behavior_group'),
    BehaviorGroupDeployment = require('../models/behavior_group_deployment'),
    Formatter = require('../lib/formatter'),
    autobind = require('../lib/autobind');

  type Props = {
    group: BehaviorGroup,
    currentUserId: string,
    isLastSavedVersion: ?boolean,
    isLastDeployedVersion: ?boolean,
    className: ?string
  };

  class BehaviorGroupSaveInfo extends React.PureComponent<Props> {
    props: Props;

    constructor(props) {
      super(props);
      autobind(this);
    }

    savedPrefix(): string {
      if (this.props.isLastSavedVersion) {
        return this.savedByCurrentUser() ? "You last saved this skill" : "Skill last saved";
      } else {
        return this.savedByCurrentUser() ? "You saved this version" : "Version saved";
  }
    }

    savedString(): string {
      const group = this.props.group;
      const ts = Formatter.formatTimestampRelativeIfRecent(group.createdAt);
      const authorName = group.author && group.author.userName ? group.author.formattedFullNameOrUserName() : null;
      const authorString = !this.savedByCurrentUser() && authorName ? `by ${authorName}` : "";
      return `${this.savedPrefix()} ${ts} ${authorString}`;
    }

    deployedPrefix(): string {
      if (this.props.isLastDeployedVersion) {
        return this.deployedByCurrentUser() ? "You last deployed this skill" : "Skill last deployed";
      } else {
        return this.deployedByCurrentUser() ? "You deployed this version" : "Version deployed";
      }
    }

    deployedString(deployment: BehaviorGroupDeployment): string {
      const ts = Formatter.formatTimestampRelativeIfRecent(deployment.createdAt);
      const deployerName = deployment.user && deployment.user.userName ? deployment.user.formattedFullNameOrUserName() : null;
      const deployerString = !this.deployedByCurrentUser() && deployerName ? `by ${deployerName}` : "";
      return `${this.deployedPrefix()} ${ts} ${deployerString}`;
    }

    savedByCurrentUser(): boolean {
      return this.props.group.author && this.props.group.author.id === this.props.currentUserId;
    }

    deployedByCurrentUser(): boolean {
      return this.props.group.deployment && this.props.group.deployment.userId === this.props.currentUserId;
    }

    statusString(): string {
      const deployment = this.props.group.deployment;
      if (deployment) {
        return this.deployedString(deployment);
      } else {
        return this.savedString();
      }
    }

    render(): React.Node {
      return (
        <span className={this.props.className || ""}>
          <span>{this.statusString()}</span>
        </span>
      );
    }
  }

  return BehaviorGroupSaveInfo;
});
