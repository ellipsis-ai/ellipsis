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

    buildStringFor(createdAt, user, isCurrentUser, prefix): string {
      const ts = Formatter.formatTimestampRelativeIfRecent(createdAt);
      const userName = user && user.userName ? user.formattedFullNameOrUserName() : null;
      const userString = !isCurrentUser && userName ? `by ${userName}` : "";
      return `${prefix} ${ts} ${userString}`;
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
      return this.buildStringFor(group.createdAt, group.author, this.savedByCurrentUser(), this.savedPrefix());
    }

    deployedPrefix(): string {
      if (this.props.isLastDeployedVersion) {
        return this.deployedByCurrentUser() ? "You last deployed this skill" : "Skill last deployed";
      } else {
        return this.deployedByCurrentUser() ? "You deployed this version" : "Version deployed";
      }
    }

    deployedString(deployment: BehaviorGroupDeployment): string {
      return this.buildStringFor(deployment.createdAt, deployment.user, this.deployedByCurrentUser(), this.deployedPrefix());
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
