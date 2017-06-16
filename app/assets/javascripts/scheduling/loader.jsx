requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './scheduling/index', 'config/scheduling/index',
      './models/scheduled_action', './models/schedule_channel', './models/behavior_group', './lib/data_request'],
    function(Core, Fetch, React, ReactDOM, Scheduling, SchedulingConfig,
             ScheduledAction, ScheduleChannel, BehaviorGroup, DataRequest) {

      let currentConfig = Object.assign({}, SchedulingConfig, {
        scheduledActions: SchedulingConfig.scheduledActions.map(ScheduledAction.fromJson),
        channelList: SchedulingConfig.channelList.map(ScheduleChannel.fromJson),
        behaviorGroups: SchedulingConfig.behaviorGroups.map(BehaviorGroup.fromJson),
        onSave: onSave
      });

      function onSave(scheduledAction) {
        const body = {
          dataJson: JSON.stringify(scheduledAction),
          teamId: SchedulingConfig.teamId
        };
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
              justSavedAction: newAction,
              scheduledActions: newActions
            });
          })
          .catch(console.log);
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
