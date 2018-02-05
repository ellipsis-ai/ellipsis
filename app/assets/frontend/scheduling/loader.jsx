/* global SchedulingConfig:false */
import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import Scheduling from './index';
import Page from '../../javascripts/shared_ui/page';
import ScheduledAction from '../models/scheduled_action';
import ScheduleChannel from '../models/schedule_channel';
import BehaviorGroup from '../../javascripts/models/behavior_group';
import DataRequest from '../../javascripts/lib/data_request';
import ImmutableObjectUtils from '../../javascripts/lib/immutable_object_utils';

const SchedulingLoader = React.createClass({
        propTypes: {
          containerId: React.PropTypes.string.isRequired,
          csrfToken: React.PropTypes.string.isRequired,
          teamId: React.PropTypes.string.isRequired,
          scheduledActions: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
          behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
          channelList: React.PropTypes.arrayOf(React.PropTypes.object),
          teamTimeZone: React.PropTypes.string,
          teamTimeZoneName: React.PropTypes.string,
          slackUserId: React.PropTypes.string,
          slackBotUserId: React.PropTypes.string,
          selectedScheduleId: React.PropTypes.string,
          newAction: React.PropTypes.bool
        },

        getInitialState: function() {
          return {
            scheduledActions: this.props.scheduledActions.map(ScheduledAction.fromJson),
            behaviorGroups: this.props.behaviorGroups.map(BehaviorGroup.fromJson),
            isSaving: false,
            justSavedAction: null,
            isDeleting: false,
            error: null
          };
        },

        onSave: function(scheduledAction) {
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
        },

        onDelete: function(scheduledAction) {
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
        },

        onClearErrors: function() {
          this.setState({
            isSaving: false,
            isDeleting: false,
            justSavedAction: null,
            error: null
          });
        },

        render: function() {
          return (
            <Page csrfToken={this.props.csrfToken}>
              <Scheduling
                scheduledActions={this.state.scheduledActions}
                channelList={this.props.channelList ? this.props.channelList.map(ScheduleChannel.fromJson) : null}
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
              />
            </Page>
          );
        }
});

ReactDOM.render(
  React.createElement(SchedulingLoader, SchedulingConfig),
  document.getElementById(SchedulingConfig.containerId)
);
