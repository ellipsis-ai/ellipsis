requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './scheduling/index', 'config/scheduling/index',
      './models/scheduled_action', './models/schedule_channel'],
    function(Core, Fetch, React, ReactDOM, Scheduling, SchedulingConfig, ScheduledAction, ScheduleChannel) {

      var config = Object.assign(SchedulingConfig, {
        scheduledActions: SchedulingConfig.scheduledActions.map(ScheduledAction.fromJson),
        channelList: SchedulingConfig.channelList.map(ScheduleChannel.fromJson)
      });

      ReactDOM.render(
        React.createElement(Scheduling, config),
        document.getElementById(SchedulingConfig.containerId)
      );
    });
});
