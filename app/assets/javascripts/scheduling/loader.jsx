requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './scheduling/index', 'config/scheduling/index',
      './models/scheduled_action', './models/schedule_channel', './models/behavior_group', './lib/data_request'],
    function(Core, Fetch, React, ReactDOM, Scheduling, SchedulingConfig,
             ScheduledAction, ScheduleChannel, BehaviorGroup, DataRequest) {

      let currentConfig = Object.assign({}, SchedulingConfig, {
        scheduledActions: SchedulingConfig.scheduledActions.map(ScheduledAction.fromJson),
        channelList: SchedulingConfig.channelList.map(ScheduleChannel.fromJson),
        behaviorGroups: SchedulingConfig.behaviorGroups.map(BehaviorGroup.fromJson),
        onSave: onSave,
        isSaving: false,
        onDelete: onDelete,
        isDeleting: false,
        onClearErrors: onClearErrors,
        error: null
      });

      function onSave(scheduledAction) {
        const body = {
          dataJson: JSON.stringify(scheduledAction),
          scheduleType: scheduledAction.scheduleType,
          teamId: SchedulingConfig.teamId
        };
        reload({
          isSaving: true,
          error: null
        });
        DataRequest.jsonPost(jsRoutes.controllers.ScheduledActionsController.save().url, body, SchedulingConfig.csrfToken)
          .then((json) => {
            const newAction = ScheduledAction.fromJson(json);
            const oldActionIndex = currentConfig.scheduledActions.findIndex((ea) => ea.id === scheduledAction.id);
            let newActions;
            if (oldActionIndex > -1) {
              newActions = currentConfig.scheduledActions.slice();
              newActions.splice(oldActionIndex, 1, newAction);
            } else {
              newActions = currentConfig.scheduledActions.concat(newAction);
            }
            reload({
              isSaving: false,
              justSavedAction: newAction,
              scheduledActions: newActions
            });
          })
          .catch((err) => {
            reload({
              isSaving: false,
              error: "An error occurred while saving. Please try again"
            });
          });
      }

      function onDelete(scheduledAction) {
        const body = {
          id: scheduledAction.id,
          scheduleType: scheduledAction.scheduleType,
          teamId: SchedulingConfig.teamId
        };
        reload({
          isDeleting: true,
          error: null
        });
        DataRequest.jsonPost(jsRoutes.controllers.ScheduledActionsController.delete().url, body, SchedulingConfig.csrfToken)
          .then((json) => {
            const oldActionIndex = currentConfig.scheduledActions.findIndex((ea) => ea.id === scheduledAction.id);
            let newActions;
            if (oldActionIndex > -1 && json.deletedId === scheduledAction.id) {
              newActions = currentConfig.scheduledActions.slice();
              newActions.splice(oldActionIndex, 1);
              reload({
                isDeleting: false,
                scheduledActions: newActions
              });
            } else {
              throw Error("No action deleted");
            }
          })
          .catch((err) => {
            reload({
              isDeleting: false,
              error: "An error occurred while deleting. Please try again"
            });
          });
      }

      function onClearErrors() {
        reload({
          isSaving: false,
          isDeleting: false,
          error: null
        });
      }

      function reload(newProps) {
        const newConfig = Object.assign({}, currentConfig, newProps);
        ReactDOM.render(
          React.createElement(Scheduling, newConfig),
          document.getElementById(SchedulingConfig.containerId)
        );
        currentConfig = newConfig;
      }

      reload();
    });
});
