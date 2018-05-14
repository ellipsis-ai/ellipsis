import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import Scheduling from './index';
import Page from '../shared_ui/page';
import ScheduledAction, {ScheduledActionJson} from '../models/scheduled_action';
import ScheduleChannel, {ScheduleChannelJson} from '../models/schedule_channel';
import BehaviorGroup, {BehaviorGroupJson} from '../models/behavior_group';
import {DataRequest} from '../lib/data_request';
import ImmutableObjectUtils from '../lib/immutable_object_utils';
import autobind from "../lib/autobind";
import User from "../models/user";

interface Props {
  containerId: string
  csrfToken: string
  teamId: string
  scheduledActions: Array<ScheduledActionJson>
  behaviorGroups: Array<BehaviorGroupJson>
  channelList: Array<ScheduleChannelJson>
  teamTimeZone: Option<string>
  teamTimeZoneName: Option<string>
  slackUserId: string
  slackBotUserId: string
  selectedScheduleId: Option<string>
  newAction: Option<boolean>
  isAdmin: boolean
}

export interface UserMap {
  [userId: string]: Option<User>
}

interface State {
  scheduledActions: Array<ScheduledAction>,
  behaviorGroups: Array<BehaviorGroup>,
  isSaving: boolean,
  justSavedAction: Option<ScheduledAction>,
  isDeleting: boolean,
  error: Option<string>,
  userMap: UserMap
}

declare var SchedulingConfig: Props;

class SchedulingLoader extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = {
      scheduledActions: this.props.scheduledActions.map(ScheduledAction.fromJson),
      behaviorGroups: this.props.behaviorGroups.map(BehaviorGroup.fromJson),
      isSaving: false,
      justSavedAction: null,
      isDeleting: false,
      error: null,
      userMap: {}
    };
  }

        onSave(scheduledAction: ScheduledAction): void {
          const body = {
            dataJson: JSON.stringify(scheduledAction),
            scheduleType: scheduledAction.scheduleType,
            teamId: this.props.teamId
          };
          this.setState({
            isSaving: true,
            justSavedAction: null,
            error: null
          }, () => {
            DataRequest.jsonPost(jsRoutes.controllers.ScheduledActionsController.save().url, body, this.props.csrfToken)
              .then((json) => {
                const newAction = ScheduledAction.fromJson(json);
                const oldActionIndex = this.state.scheduledActions.findIndex((ea) => ea.id === scheduledAction.id);
                let newActions;
                if (oldActionIndex > -1) {
                  newActions = ImmutableObjectUtils.arrayWithNewElementAtIndex(this.state.scheduledActions, newAction, oldActionIndex);
                } else {
                  newActions = this.state.scheduledActions.concat(newAction);
                }
                this.setState({
                  isSaving: false,
                  justSavedAction: newAction,
                  scheduledActions: newActions
                });
              })
              .catch(() => {
                this.setState({
                  isSaving: false,
                  error: "An error occurred while saving. Please try again"
                });
              });
          });
        }

        onDelete(scheduledAction: ScheduledAction): void {
          const body = {
            id: scheduledAction.id,
            scheduleType: scheduledAction.scheduleType,
            teamId: this.props.teamId
          };
          this.setState({
            isDeleting: true,
            justSavedAction: null,
            error: null
          }, () => {
            DataRequest.jsonPost(jsRoutes.controllers.ScheduledActionsController.delete().url, body, this.props.csrfToken)
              .then((json) => {
                const oldActionIndex = this.state.scheduledActions.findIndex((ea) => ea.id === scheduledAction.id);
                if (oldActionIndex > -1 && json.deletedId === scheduledAction.id) {
                  this.setState({
                    isDeleting: false,
                    scheduledActions: ImmutableObjectUtils.arrayRemoveElementAtIndex(this.state.scheduledActions, oldActionIndex)
                  });
                } else {
                  throw Error("No action deleted");
                }
              })
              .catch(() => {
                this.setState({
                  isDeleting: false,
                  error: "An error occurred while deleting. Please try again"
                });
              });
          });
        }

        onLoadUserData(userId: string): void {
          if (this.props.isAdmin) {
            const url = jsRoutes.controllers.admin.UserInfoController.userDataFor(userId).url;
            DataRequest.jsonGet(url).then((json) => {
              if (json) {
                const userData = {};
                userData[userId] = User.fromJson(json);
                this.setState({
                  userMap: Object.assign({}, this.state.userMap, userData)
                });
              }
            });
          }
        }

        onClearErrors(): void {
          this.setState({
            isSaving: false,
            isDeleting: false,
            justSavedAction: null,
            error: null
          });
        }

        render() {
          return (
            <Page csrfToken={this.props.csrfToken}
              onRender={(pageProps) => (
              <Scheduling
                scheduledActions={this.state.scheduledActions}
                channelList={this.props.channelList ? this.props.channelList.map(ScheduleChannel.fromJson) : []}
                behaviorGroups={this.state.behaviorGroups}
                onSave={this.onSave}
                isSaving={this.state.isSaving}
                justSavedAction={this.state.justSavedAction}
                onDelete={this.onDelete}
                isDeleting={this.state.isDeleting}
                onClearErrors={this.onClearErrors}
                error={this.state.error}
                teamId={this.props.teamId}
                teamTimeZone={this.props.teamTimeZone}
                teamTimeZoneName={this.props.teamTimeZoneName}
                slackUserId={this.props.slackUserId}
                slackBotUserId={this.props.slackBotUserId}
                selectedScheduleId={this.props.selectedScheduleId}
                newAction={this.props.newAction}
                isAdmin={this.props.isAdmin}
                userMap={this.state.userMap}
                onLoadUserData={this.onLoadUserData}
                {...pageProps}
              />
            )} />
          );
        }
}

if (typeof SchedulingConfig !== "undefined") {
  const container = document.getElementById(SchedulingConfig.containerId);
  if (container) {
    ReactDOM.render((
      <SchedulingLoader {...SchedulingConfig} />
    ), container);
  }
}
