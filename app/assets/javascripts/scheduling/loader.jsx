requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './scheduling/index', 'config/scheduling/index',
      './models/scheduled_action', './models/schedule_channel', './lib/data_request'],
    function(Core, Fetch, React, ReactDOM, Scheduling, SchedulingConfig, ScheduledAction, ScheduleChannel, DataRequest) {

      function onSave(scheduledAction) {
        const body = {
          dataJson: JSON.stringify(scheduledAction),
          teamId: SchedulingConfig.teamId
        };
        DataRequest.jsonPost(jsRoutes.controllers.ScheduledActionsController.save().url, body, SchedulingConfig.csrfToken)
          .then((result) => { console.log(result); })
          .catch((err) => { console.log(err); });
      }

      var config = Object.assign(SchedulingConfig, {
        scheduledActions: SchedulingConfig.scheduledActions.map(ScheduledAction.fromJson),
        channelList: SchedulingConfig.channelList.map(ScheduleChannel.fromJson),
        onSave: onSave
      });

      ReactDOM.render(
        React.createElement(Scheduling, config),
        document.getElementById(SchedulingConfig.containerId)
      );
    });
});
