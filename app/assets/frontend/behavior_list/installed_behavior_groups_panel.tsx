import * as React from 'react';
import Button from '../form/button';
import SVGCheckmark from '../svg/checkmark';
import URLCreator from '../lib/url_creator';
import BehaviorGroup from '../models/behavior_group';
import autobind from '../lib/autobind';
import DynamicLabelButton from "../form/dynamic_label_button";
import Collapsible from "../shared_ui/collapsible";

type Props = {
  installedBehaviorGroup: Option<BehaviorGroup>,
  onToggle: () => void,
  onDeploy: (behaviorGroupId: string) => void,
  isDeploying: boolean,
  deployError: Option<string>,
  slackTeamId: string,
  botName: string
}

class InstalledBehaviorGroupsPanel extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  getInstalledBehaviorGroup(): Option<BehaviorGroup> {
    return this.props.installedBehaviorGroup;
  }

  getHeading(group: BehaviorGroup) {
    var groupName = group.getName();
    return (
      <span>You have installed a new skill: <b>{groupName}</b></span>
    );
  }

  getBehaviorHelpInstructions(group: BehaviorGroup) {
    return (
      <li>
        <span>In your channel, type <span className="box-chat-example">{this.getBotNameForSlack()} help {group.getName()}</span> </span>
        <span>to try out the {group.getName()} skill, or </span>
        <span className="box-chat-example">{this.getBotNameForSlack()} help</span>
        <span> to see everything your bot can do so far.</span>
      </li>
    );
  }

  getSlackUrl() {
    return URLCreator.forSlack(this.props.slackTeamId);
  }

  getBotNameForSlack(): string {
    return `@${this.props.botName}`;
  }

  renderEditLinkFor(group: BehaviorGroup) {
    if (group.id) {
      const url = jsRoutes.controllers.BehaviorEditorController.edit(group.id).url;
      return (
        <a className="button mbs mrs" href={url}>Edit skill</a>
      );
    } else {
      return null;
    }
  }

  onClickDone() {
    this.props.onToggle();
  }

  deploy() {
    if (this.props.installedBehaviorGroup && this.props.installedBehaviorGroup.id) {
      this.props.onDeploy(this.props.installedBehaviorGroup.id);
    }
  }

  render() {
    const group = this.getInstalledBehaviorGroup();
    if (!group) {
      return null;
    }
    const groupIsDeployed = Boolean(group.deployment);
    return (
      <div className="bg-green-lightest border-emphasis-top border-green">
        <div className="bg-green-light">
          <div className="container container-c pvm">
              <span className="type-green display-inline-block mrs align-b" style={{ height: "24px" }}>
                <SVGCheckmark/>
              </span>
            {this.getHeading(group)}
          </div>
        </div>

        <div className="container container-c pvxl">

          <Collapsible revealWhen={!groupIsDeployed}>
            <p>
              To start using your skill right away, deploy it to your team now. Or you can edit it first and deploy later.
            </p>
          </Collapsible>

          <Collapsible revealWhen={groupIsDeployed}>

            <p>To use this skill now:</p>

            <ul className="list-space-xl">
              <li>
                <span>Type <span className="box-chat-example">/invite {this.getBotNameForSlack()}</span> to add your </span>
                <span>bot to any channel.</span>
              </li>
              {this.getBehaviorHelpInstructions(group)}
            </ul>

            <div className="mvxl">
              <a className="button mbs mrs button-primary" href={this.getSlackUrl()}>Open your Slack team</a>
            </div>

          </Collapsible>

          <div className="mtxl">
            <DynamicLabelButton
              className="button-primary mbs mrs"
              onClick={this.deploy}
              disabledWhen={this.props.isDeploying || groupIsDeployed}
              labels={[{
                text: "Deploy…",
                displayWhen: !this.props.isDeploying && !groupIsDeployed
              }, {
                text: "Deploying",
                displayWhen: this.props.isDeploying
              }, {
                text: "Deployed",
                displayWhen: !this.props.isDeploying && groupIsDeployed
              }]}
            />

            {this.renderEditLinkFor(group)}

            <Button className="mbs mrl" onClick={this.onClickDone}>Done</Button>

            {this.props.deployError ? (
              <div className="align-button mbs type-pink type-bold type-italic fade-in">
                {this.props.deployError}
              </div>
            ) : null}
          </div>

        </div>
      </div>
    );
  }
}

export default InstalledBehaviorGroupsPanel;
