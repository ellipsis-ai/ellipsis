/* global ColorsConfig:false */

requirejs(['../common'], function() {
  requirejs(['core-js', 'react', 'react-dom', './styleguide/colors/index'],
    function(Core, React, ReactDOM, Colors) {
      var config = ColorsConfig;
      var myApplicationList = React.createElement(Colors, config);
      ReactDOM.render(myApplicationList, document.getElementById(config.containerId));
    });
});
