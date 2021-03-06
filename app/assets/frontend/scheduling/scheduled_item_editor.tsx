import * as React from 'react';
import RecurrenceEditor from './recurrence_editor';
import SectionHeading from '../shared_ui/section_heading';
import ScheduleChannelEditor from './schedule_channel_editor';
import ScheduledItemConfig from './scheduled_item_config';
import BehaviorGroup from '../models/behavior_group';
import ScheduledAction, {ScheduledActionArgument, ScheduleType} from '../models/scheduled_action';
import Recurrence from "../models/recurrence";
import autobind from "../lib/autobind";
import User from "../models/user";
import OrgChannels from "../models/org_channels";

interface Props {
  isForSingleGroup: boolean
  groupId: Option<string>
  teamId: string
  scheduledAction: Option<ScheduledAction>,
  orgChannels: OrgChannels,
  behaviorGroups: Array<BehaviorGroup>,
  onChange: (newItem: ScheduledAction, optionalCallback?: () => void) => void,
  teamTimeZone: string,
  teamTimeZoneName: string,
  slackUserId: string,
  slackBotUserId: string,
  isAdmin: boolean,
  scheduleUser: Option<User>,
  userTimeZoneName: Option<string>
  csrfToken: string
}

class ScheduledItemEditor extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  updateRecurrence(newRecurrence: Recurrence): void {
    if (this.props.scheduledAction) {
      this.props.onChange(this.props.scheduledAction.clone({
        recurrence: newRecurrence
      }));
    }
  }

  updateTriggerText(newText: string): void {
    if (this.props.scheduledAction) {
      this.props.onChange(this.props.scheduledAction.clone({
        trigger: newText
      }));
    }
  }

  updateAction(behaviorId: string, newArgs: Array<ScheduledActionArgument>, callback?: () => void): void {
    if (this.props.scheduledAction) {
      this.props.onChange(this.props.scheduledAction.clone({
        behaviorId: behaviorId,
        arguments: newArgs
      }), callback);
    }
  }

  updateSkill(behaviorGroupId: string): void {
    if (this.props.scheduledAction) {
      this.props.onChange(this.props.scheduledAction.clone({
        behaviorGroupId: behaviorGroupId,
        behaviorId: null
      }));
    }
  }

  toggleByTrigger(byTrigger: boolean): void {
    if (this.props.scheduledAction) {
      this.props.onChange(this.props.scheduledAction.clone({
        trigger: byTrigger ? "" : null,
        behaviorGroupId: this.props.groupId || this.props.scheduledAction.behaviorGroupId,
        behaviorId: null,
        arguments: [],
        scheduleType: byTrigger ? ScheduleType.Message : ScheduleType.Behavior
      }));
    }
  }

  updateChannel(channelId: string, useDM: boolean): void {
    if (this.props.scheduledAction) {
      this.props.onChange(this.props.scheduledAction.clone({
        channel: channelId,
        useDM: useDM
      }));
    }
  }

  classNameForHeading(): string {
    return `column ${this.props.isForSingleGroup ? "ptxxl column-full" : "column-page-sidebar pvxxl mobile-pbn"}`;
  }

  classNameForEditComponent(): string {
    return `column ${this.props.isForSingleGroup ? "pbxxl column-full" : "column-page-main pvxxl mobile-ptn"}`;
  }

  renderDetails(scheduledAction: ScheduledAction) {
    return (
      <div>
        <div>
          {this.props.isAdmin ? (
            <div className="bg-light type-weak">
              <div className="container container-wide pvm">
                <h5>Admin info</h5>
                <div>
                  <span>Scheduled by </span>
                  {this.props.scheduleUser ? (
                    <span>user {this.props.scheduleUser.formattedName()} on team {this.props.scheduleUser.teamName || "(unknown)"} · </span>
                  ) : null}
                  {this.props.scheduleUser && this.props.scheduleUser.email ? (
                    <span><a href={`mailto:${this.props.scheduleUser.email}`}>{this.props.scheduleUser.email}</a> · </span>
                  ) : null}
                  <span>Ellipsis user ID {scheduledAction.userId || "(none)"} </span>
                </div>
              </div>
            </div>
          ) : null}

          <div className="columns border-bottom">
            <div className={this.classNameForHeading()}>
              <div className="container">
                <SectionHeading number="1">What to do</SectionHeading>
              </div>
            </div>
            <div className={this.classNameForEditComponent()}>
              <div className="container container-wide">
                <ScheduledItemConfig
                  teamId={this.props.teamId}
                  csrfToken={this.props.csrfToken}
                  scheduledAction={scheduledAction}
                  behaviorGroups={this.props.behaviorGroups}
                  onChangeTriggerText={this.updateTriggerText}
                  onChangeAction={this.updateAction}
                  onChangeSkill={this.updateSkill}
                  onToggleByTrigger={this.toggleByTrigger}
                  isForSingleGroup={this.props.isForSingleGroup}
                  groupId={this.props.groupId}
                />
              </div>
            </div>
          </div>

          <div className="columns border-bottom">
            <div className={this.classNameForHeading()}>
              <div className="container">
                <SectionHeading number="2">Where to do it</SectionHeading>
              </div>
            </div>
            <div className={this.classNameForEditComponent()}>
              <div className="container container-wide">
                <ScheduleChannelEditor
                  scheduledAction={scheduledAction}
                  orgChannels={this.props.orgChannels}
                  onChange={this.updateChannel}
                  slackUserId={this.props.slackUserId}
                  slackBotUserId={this.props.slackBotUserId}
                />
              </div>
            </div>
          </div>

          <div className="columns">
            <div className={this.classNameForHeading()}>
              <div className="container">
                <SectionHeading number="3">When to do it</SectionHeading>
              </div>
            </div>
            <div className={this.classNameForEditComponent()}>
              <div className="container container-wide">
                <RecurrenceEditor
                  onChange={this.updateRecurrence}
                  scheduledAction={scheduledAction}
                  teamTimeZone={this.props.teamTimeZone}
                  teamTimeZoneName={this.props.teamTimeZoneName}
                  userTimeZoneName={this.props.userTimeZoneName}
                  csrfToken={this.props.csrfToken}
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  render() {
    const scheduledAction = this.props.scheduledAction;
    if (scheduledAction) {
      return this.renderDetails(scheduledAction);
    } else {
      return null;
    }
  }
}

export default ScheduledItemEditor;
