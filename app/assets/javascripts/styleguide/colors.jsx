/* global ColorsConfig:false */

requirejs(['../common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './styleguide/colors/index'],
    function(Core, Fetch, React, ReactDOM, Colors) {
      var config = ColorsConfig;
      var myApplicationList = React.createElement(Colors, config);
      ReactDOM.render(myApplicationList, document.getElementById(config.containerId));
    });
});
