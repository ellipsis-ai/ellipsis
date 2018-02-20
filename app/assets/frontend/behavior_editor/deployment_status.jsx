// @flow
import * as React from 'react';
import DynamicLabelButton from '../form/dynamic_label_button';
import HelpButton from '../help/help_button';
import SVGInfo from '../svg/info';
import SVGCheckmark from '../svg/checkmark';
import moment from 'moment';
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

  durationSinceDeployment(): ?string {
    const lastSaved = this.props.lastSaveTimestamp;
    const lastDeployed = this.props.lastDeployTimestamp;
    return lastSaved && lastDeployed ?
      moment(lastDeployed).from(new Date(lastSaved), true) :
      null;
  }

  undeployedStatus() {
    const saved = this.props.lastSaveTimestamp ? Formatter.formatTimestampRelativeIfRecent(this.props.lastSaveTimestamp) : null;
    const deployed = this.durationSinceDeployment();
    return (
      <span>
        <span>{saved ? `Saved ${saved}` : "Never saved"}</span>
        <span className="type-weak mhxs">·</span>
        {deployed ? (
          <span>Last deployed {deployed} before</span>
        ) : (
          <b>Not yet deployed</b>
        )}
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

  userNameFor(user: ?User): string {
    if (!user) {
      return "unknown";
    } else if (user.id === this.props.currentUserId) {
      return "you";
    } else {
      return user.formattedFullNameOrUserName();
    }
  }

  getFullStatus(isExisting: boolean, isModified: boolean, currentDeployment: ?BehaviorGroupDeployment): string {
    const lastSaved = isExisting && this.props.lastSaveTimestamp ? Formatter.formatTimestampShort(this.props.lastSaveTimestamp) : null;
    const lastSavedText = lastSaved ? `Last saved: ${lastSaved} by ${this.userNameFor(this.props.group.author)}` : "Skill never saved";

    const lastDeployedTimestamp = currentDeployment ? currentDeployment.createdAt : this.props.lastDeployTimestamp;
    const lastDeployed = lastDeployedTimestamp ? Formatter.formatTimestampShort(lastDeployedTimestamp) : null;
    const lastDeployedUser = currentDeployment ? ` by ${this.userNameFor(currentDeployment.deployer)}` : "";
    const lastDeployedText = lastDeployed ? `Last deployed: ${lastDeployed}${lastDeployedUser}` : "Skill never deployed";

    const unsavedChangeText = isExisting && isModified ? "There are unsaved changes." : "";
    return [lastSavedText, lastDeployedText, unsavedChangeText].join("\n");
  }

  renderStatus(isExisting: boolean, isModified: boolean, currentDeployment: ?BehaviorGroupDeployment): React.Node {
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
          <span className="display-inline-block align-button-s">Deployed</span>
        </div>
      );
    } else {
      return (
        <div className="fade-in">
          <div className="display-inline-block align-m height-xl type-yellow mrs"><SVGInfo label={this.getFullStatus(isExisting, isModified, currentDeployment)} /></div>
          <div className="display-inline-block align-button-s">{this.undeployedStatus()}</div>
        </div>
      );
    }
  }

  render(): React.Node {
    const isExisting = Boolean(this.props.group.createdAt && this.props.group.id);
    const isModified = this.props.isModified;
    const currentDeployment = this.props.group.deployment;
    return (
      <div className="display-nowrap">
        <div className="display-inline-block mrm" title={this.getFullStatus(isExisting, isModified, currentDeployment)}>
          {this.renderStatus(isExisting, isModified, currentDeployment)}
        </div>
        <DynamicLabelButton
          className="button-s button-shrink mrs"
          onClick={this.deploy}
          labels={this.getConfirmButtonLabels()}
          disabledWhen={this.isDeploying() || Boolean(currentDeployment) || isModified || !isExisting}
        />
        <HelpButton onClick={this.onHelpClick}/>
      </div>
    );
  }
}

export default DeploymentStatus;

