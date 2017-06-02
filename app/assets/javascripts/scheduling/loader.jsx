requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './scheduling/index', 'config/scheduling/index', './models/scheduled_action'],
    function(Core, Fetch, React, ReactDOM, Scheduling, SchedulingConfig, ScheduledAction) {

      var config = Object.assign(SchedulingConfig, {
        scheduledActions: SchedulingConfig.scheduledActions.map(ScheduledAction.fromJson)
      });

      ReactDOM.render(
        React.createElement(Scheduling, config),
        document.getElementById(SchedulingConfig.containerId)
      );
    });
});
