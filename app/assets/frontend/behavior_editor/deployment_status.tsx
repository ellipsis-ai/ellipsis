import * as React from 'react';
import DynamicLabelButton from '../form/dynamic_label_button';
import HelpButton from '../help/help_button';
import SVGInfo from '../svg/info';
import SVGCheckmark from '../svg/checkmark';
import * as moment from 'moment';
import autobind from '../lib/autobind';
import BehaviorGroup from "../models/behavior_group";
import BehaviorGroupDeployment from "../models/behavior_group_deployment";
import User from '../models/user';
import Formatter from "../lib/formatter";

type Props = {
  group: BehaviorGroup,
  isModified: boolean,
  lastSaveTimestamp?: string,
  lastDeployTimestamp?: string,
  currentUserId: string,
  onDevModeChannelsClick: () => void,
  onDeployClick: (callback: () => void) => void,
}

type State = {
  isDeploying: boolean
}

class DeploymentStatus extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = {
      isDeploying: false
    };
  }

  deploy(): void {
    this.setState({
      isDeploying: true
    }, () => {
      this.props.onDeployClick(() => {
        this.setState({ isDeploying: false });
      });
    });
  }

  isDeploying(): boolean {
    return this.state.isDeploying;
  }

  saveStatus(): string {
    const timestamp = this.props.lastSaveTimestamp;
    if (!timestamp || !this.props.group.isExisting()) {
      return "Unsaved";
    }
    const savedMoment = moment(timestamp);
    const nowMoment = moment();
    if (savedMoment.isSame(nowMoment, 'day')) {
      return `Saved ${Formatter.formatTimestampRelative(timestamp)}`;
    } else {
      return `Last modified ${Formatter.formatTimestampDate(timestamp)}`;
    }
  }

  lastDeployedStatus() {
    if (this.props.lastDeployTimestamp) {
      return (
        <span>Older version deployed</span>
      );
    } else {
      return (
        <b>Not yet deployed</b>
      );
    }
  }

  undeployedStatus() {
    return (
      <span>
        <span className="display-inline-block">{this.saveStatus()}</span>
        <span className="display-inline-block type-weak mhxs">·</span>
        <span className="display-inline-block">{this.lastDeployedStatus()}</span>
      </span>
    );
  }

  getConfirmButtonLabels(): Array<{ text: string, displayWhen: boolean }> {
    return [
      {
        text: "Deploying…",
        displayWhen: this.isDeploying()
      },
      {
        text: "Deploy…",
        displayWhen: !this.isDeploying()
      }
    ];
  }

  onHelpClick(): void {
    this.props.onDevModeChannelsClick();
  }

  userNameFor(user: Option<User>): string {
    if (!user) {
      return "unknown";
    } else if (user.id === this.props.currentUserId) {
      return "you";
    } else {
      return user.formattedFullNameOrUserName();
    }
  }

  getFullStatus(isExisting: boolean, isModified: boolean, currentDeployment: Option<BehaviorGroupDeployment>): string {
    const lastSaved = isExisting && this.props.lastSaveTimestamp ? Formatter.formatTimestampShort(this.props.lastSaveTimestamp) : null;
    const lastSavedText = lastSaved ? `Last saved: ${lastSaved} by ${this.userNameFor(this.props.group.author)}` : "Skill never saved";

    const lastDeployedTimestamp = currentDeployment ? currentDeployment.createdAt : this.props.lastDeployTimestamp;
    const lastDeployed = lastDeployedTimestamp ? Formatter.formatTimestampShort(lastDeployedTimestamp) : null;
    const lastDeployedUser = currentDeployment ? ` by ${this.userNameFor(currentDeployment.deployer)}` : "";
    const lastDeployedText = lastDeployed ? `Last deployed: ${lastDeployed}${lastDeployedUser}` : "Skill never deployed";

    const unsavedChangeText = isExisting && isModified ? "There are unsaved changes." : "";
    return [lastSavedText, lastDeployedText, unsavedChangeText].join("\n");
  }

  renderStatus(isExisting: boolean, isModified: boolean, currentDeployment: Option<BehaviorGroupDeployment>) {
    if (isExisting && isModified) {
      return (
        <div className="fade-in">
          <span className="align-button-s type-pink type-italic type-bold">Unsaved changes</span>
        </div>
      );
    } else if (currentDeployment) {
      return (
        <div className="fade-in">
          <span className="display-inline-block height-xl mrs align-m type-green"><SVGCheckmark label={this.getFullStatus(isExisting, isModified, currentDeployment)}/></span>
          <span className="type-s">Deployed</span>
        </div>
      );
    } else {
      return (
        <div className="fade-in">
          <span className="display-inline-block align-m height-xl type-yellow mrs"><SVGInfo label={this.getFullStatus(isExisting, isModified, currentDeployment)} /></span>
          <span className="type-s">{this.undeployedStatus()}</span>
        </div>
      );
    }
  }

  render() {
    const isExisting = Boolean(this.props.group.createdAt && this.props.group.id);
    const isModified = this.props.isModified;
    const currentDeployment = this.props.group.deployment;
    return (
      <div>
        <div className="type-s display-inline-block mrm mvl" title={this.getFullStatus(isExisting, isModified, currentDeployment)}>
          {this.renderStatus(isExisting, isModified, currentDeployment)}
        </div>
        <div className="type-m display-inline-block mbl">
          {currentDeployment && !isModified ? null : (
            <DynamicLabelButton
              className="button-s button-shrink mrs"
              onClick={this.deploy}
              labels={this.getConfirmButtonLabels()}
              disabledWhen={this.isDeploying() || isModified || !isExisting}
            />
          )}
          <HelpButton onClick={this.onHelpClick} />
        </div>
      </div>
    );
  }
}

export default DeploymentStatus;

