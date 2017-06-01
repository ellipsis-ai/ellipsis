requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './scheduling/index', 'config/scheduling/index'],
    function(Core, Fetch, React, ReactDOM, Scheduling, SchedulingConfig) {
      ReactDOM.render(
        React.createElement(Scheduling, SchedulingConfig),
        document.getElementById(SchedulingConfig.containerId)
      );
    });
});
