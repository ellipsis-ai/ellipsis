import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import Scheduling from './index';
import Page from '../shared_ui/page';
import ScheduledAction, {ScheduledActionJson} from '../models/scheduled_action';
import BehaviorGroup, {BehaviorGroupJson} from '../models/behavior_group';
import {DataRequest} from '../lib/data_request';
import ImmutableObjectUtils from '../lib/immutable_object_utils';
import autobind from "../lib/autobind";
import User, {UserJson} from "../models/user";
import OrgChannels, {OrgChannelsJson} from "../models/org_channels";
import Trigger, {TriggerJson} from "../models/trigger";

interface Props {
  containerId: string
  csrfToken: string
  teamId: string
  scheduledActions: Array<ScheduledActionJson>
  behaviorGroups: Array<BehaviorGroupJson>
  orgChannels: OrgChannelsJson,
  teamTimeZone: Option<string>
  teamTimeZoneName: Option<string>
  slackUserId: string
  slackBotUserId: string
  selectedScheduleId: Option<string>
  filterChannelId: Option<string>
  filterBehaviorGroupId: Option<string>
  newAction: Option<boolean>
  isAdmin: boolean
}

export interface UserMap {
  [userId: string]: Option<User>
}

export interface ValidBehaviorIdTriggerJson {
  behaviorId: string
  triggers: Array<TriggerJson>
}

export interface ValidBehaviorIdTriggerInterface extends ValidBehaviorIdTriggerJson {
  triggers: Array<Trigger>
}

export interface ValidTriggerJson {
  text: string
  matchingBehaviorTriggers: Array<ValidBehaviorIdTriggerJson>
}

export interface ValidTriggerInterface extends ValidTriggerJson {
  matchingBehaviorTriggers: Array<ValidBehaviorIdTriggerInterface>
}

interface State {
  scheduledActions: Array<ScheduledAction>,
  validTriggers: Array<ValidTriggerInterface>
  behaviorGroups: Array<BehaviorGroup>,
  isSaving: boolean,
  justSavedAction: Option<ScheduledAction>,
  isDeleting: boolean,
  error: Option<string>,
  userMap: UserMap,
  isValidatingTriggers: boolean,
  triggerValidationError: Option<string>
}

declare var SchedulingConfig: Props;

class SchedulingLoader extends React.Component<Props, State> {
  triggerValidationTimer: number | undefined;

  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = {
      scheduledActions: this.props.scheduledActions.map(ScheduledAction.fromJson),
      validTriggers: [],
      behaviorGroups: this.props.behaviorGroups.map(BehaviorGroup.fromJson),
      isSaving: false,
      justSavedAction: null,
      isDeleting: false,
      error: null,
      userMap: {},
      isValidatingTriggers: false,
      triggerValidationError: null
    };
  }

  componentDidMount(): void {
    this.startTriggerValidationTimer();
    window.addEventListener('blur', this.clearTriggerValidationTimer);
    window.addEventListener('focus', this.startTriggerValidationTimer);
  }

  componentWillUnmount(): void {
    this.clearTriggerValidationTimer();
    window.removeEventListener('blur', this.clearTriggerValidationTimer);
    window.removeEventListener('focus', this.startTriggerValidationTimer);
  }

  startTriggerValidationTimer(): void {
    this.resetTriggerValidation();
    this.triggerValidationTimer = setInterval(this.resetTriggerValidation, 30000);
  }

  clearTriggerValidationTimer(): void {
    clearInterval(this.triggerValidationTimer);
  }

  resetTriggerValidation(): void {
    const possibleTriggers = this.state.scheduledActions.map((ea) => ea.trigger).filter((ea): ea is string => Boolean(ea));
    const uniqueTriggers = Array.from(new Set(possibleTriggers));
    if (uniqueTriggers.length > 0) {
      this.setState({ isValidatingTriggers: true });
      DataRequest.jsonPost(jsRoutes.controllers.ScheduledActionsController.validateTriggers().url, {
        triggerMessages: uniqueTriggers,
        teamId: this.props.teamId
      }, this.props.csrfToken).then((results: Array<ValidTriggerJson>) => {
        this.setState({
          isValidatingTriggers: false,
          validTriggers: results.map((result) => {
            return {
              text: result.text,
              matchingBehaviorTriggers: result.matchingBehaviorTriggers.map((behaviorTriggers) => {
                return {
                  behaviorId: behaviorTriggers.behaviorId,
                  triggers: behaviorTriggers.triggers.map(Trigger.fromJson)
                };
              })
            };
          })
        });
      }).catch(() => {
        this.setState({
          isValidatingTriggers: false,
          triggerValidationError: "An error occurred while trying to validate scheduled triggers."
        });
      });
    } else {
      this.setState({
        validTriggers: [],
        triggerValidationError: null
      });
    }
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
                this.resetTriggerValidation();
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
                  this.resetTriggerValidation();
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
            DataRequest.jsonGet(url).then((userResponse: { user: Option<UserJson>, userNotFound: boolean }) => {
              const userData: {
                [userId: string]: User
              } = {};
              if (userResponse.user) {
                userData[userId] = User.fromJson(userResponse.user);
                this.updateUserMap(userData);
              } else if (userResponse.userNotFound) {
                userData[userId] = User.withoutProfile(userId);
                this.updateUserMap(userData);
              }
            }).catch(() => {
              // Ignore errors since this is admin-only for now
            });
          }
        }

        updateUserMap(userData: UserMap) {
          this.setState({
            userMap: Object.assign({}, this.state.userMap, userData)
          });
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
                orgChannels={OrgChannels.fromJson(this.props.orgChannels)}
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
                filterChannelId={this.props.filterChannelId}
                filterBehaviorGroupId={this.props.filterBehaviorGroupId}
                newAction={this.props.newAction}
                isAdmin={this.props.isAdmin}
                userMap={this.state.userMap}
                onLoadUserData={this.onLoadUserData}
                csrfToken={this.props.csrfToken}
                validTriggers={this.state.validTriggers}
                isValidatingTriggers={this.state.isValidatingTriggers}
                triggerValidationError={this.state.triggerValidationError}
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
