import * as React from 'react';
import RecurrenceEditor from './recurrence_editor';
import SectionHeading from '../shared_ui/section_heading';
import ScheduleChannelEditor from './schedule_channel_editor';
import ScheduledItemConfig from './scheduled_item_config';
import BehaviorGroup from '../models/behavior_group';
import ScheduledAction, {ScheduledActionArgument} from '../models/scheduled_action';
import ScheduleChannel from '../models/schedule_channel';
import Recurrence from "../models/recurrence";
import autobind from "../lib/autobind";
import User from "../models/user";

interface Props {
  scheduledAction: Option<ScheduledAction>,
  channelList: Array<ScheduleChannel>,
  behaviorGroups: Array<BehaviorGroup>,
  onChange: (newItem: ScheduledAction, optionalCallback?: () => void) => void,
  teamTimeZone: string,
  teamTimeZoneName: string,
  slackUserId: string,
  slackBotUserId: string,
  isAdmin: boolean,
  scheduleUser: Option<User>,
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

  updateChannel(channelId: string, useDM: boolean): void {
    if (this.props.scheduledAction) {
      this.props.onChange(this.props.scheduledAction.clone({
        channel: channelId,
        useDM: useDM
      }));
    }
  }

  renderDetails(scheduledAction: ScheduledAction) {
    return (
      <div>
        <div className="pbxxxxl">
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
          <div className="container container-wide border-bottom pvxxl">
            <SectionHeading number="1">What to do</SectionHeading>
            <ScheduledItemConfig
              scheduledAction={scheduledAction}
              behaviorGroups={this.props.behaviorGroups}
              onChangeTriggerText={this.updateTriggerText}
              onChangeAction={this.updateAction}
            />
          </div>
          <div className="container container-wide border-bottom pvxxl">
            <SectionHeading number="2">Where to do it</SectionHeading>
            <ScheduleChannelEditor
              scheduledAction={scheduledAction}
              channelList={this.props.channelList}
              onChange={this.updateChannel}
              slackUserId={this.props.slackUserId}
              slackBotUserId={this.props.slackBotUserId}
            />
          </div>
          <div className="container container-wide pvxxl">
            <SectionHeading number="3">When to do it</SectionHeading>
            <RecurrenceEditor
              onChange={this.updateRecurrence}
              recurrence={scheduledAction.recurrence}
              teamTimeZone={this.props.teamTimeZone}
              teamTimeZoneName={this.props.teamTimeZoneName}
              csrfToken={this.props.csrfToken}
            />
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
