// @flow
import * as React from 'react';
import SVGCheckmark from '../svg/checkmark';
import URLCreator from '../lib/url_creator';
import BehaviorGroup from '../models/behavior_group';
import autobind from '../lib/autobind';

type Props = {
  installedBehaviorGroup: ?BehaviorGroup,
  onToggle: () => void,
  slackTeamId: string,
  botName: string
}

class InstalledBehaviorGroupsPanel extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  getInstalledBehaviorGroup(): ?BehaviorGroup {
    return this.props.installedBehaviorGroup;
  }

  getHeading(group: BehaviorGroup) {
    var groupName = group.getName();
    return (
      <span>Ellipsis has a learned new skill: <b>{groupName}</b></span>
    );
  }

  getIntro() {
    return (
      <span>Use your bot’s new skill:</span>
    );
  }

  getFirstTriggerFor(group: BehaviorGroup) {
    var triggerText = "";
    group.behaviorVersions.find((version) => {
      return version.triggers.some((trigger) => {
        if (!trigger.isRegex && /\bhelp\b/i.test(trigger.text)) {
          triggerText = trigger.displayText();
          return true;
        } else {
          return false;
        }
      });
    });
    if (!triggerText) {
      group.behaviorVersions.find((version) => {
        return version.triggers.find((trigger) => {
          if (trigger.text) {
            triggerText = trigger.displayText();
            return true;
          } else {
            return false;
          }
        });
      });
    }
    return triggerText;
  }

  getBehaviorHelpInstructions(group: BehaviorGroup) {
    var firstTrigger = this.getFirstTriggerFor(group);
    if (firstTrigger) {
      return (
        <li>
          <span>In your channel, type <span className="box-chat-example">{firstTrigger}</span> </span>
          <span>to try out the {group.getName()} skill, or </span>
          <span className="box-chat-example">...help</span>
          <span> to see everything Ellipsis can do so far.</span>
        </li>
      );
    } else {
      return (
        <li>
          <span>In your channel, type <span className="box-chat-example">...help</span> to see everything Ellipsis can do so far.</span>
        </li>
      );
    }
  }

  getSlackUrl() {
    return URLCreator.forSlack(this.props.slackTeamId);
  }

  getEditUrlFor(group: BehaviorGroup) {
    return jsRoutes.controllers.BehaviorEditorController.edit(group.id).url;
  }

  onClickDone() {
    this.props.onToggle();
  }

  render() {
    const group = this.getInstalledBehaviorGroup();
    if (!group) {
      return null;
    }
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
          <div className="mbxl">{this.getIntro()}</div>

          <ul className="list-space-xl">
            <li>
              <span>Type <span className="box-chat-example">/invite @{this.props.botName}</span> to add your </span>
              <span>bot to any channel.</span>
            </li>
            {this.getBehaviorHelpInstructions(group)}
          </ul>

          <div className="mtxl">
            <button type="button" className="mbs mrs" onClick={this.onClickDone}>Done</button>

            <a className="button mbs mrs button-primary" href={this.getSlackUrl()}>Open your Slack team</a>

            <a className="button mbs mrs" href={this.getEditUrlFor(group)}>Edit {group.getName()}</a>
          </div>

        </div>
      </div>
    );
  }
}

export default InstalledBehaviorGroupsPanel;
