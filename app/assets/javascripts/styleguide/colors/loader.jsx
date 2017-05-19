requirejs(['common'], function() {
  requirejs(
    ['core-js', 'whatwg-fetch', 'react', 'react-dom', './styleguide/colors/index', 'config/styleguide/colors'],
    function(Core, Fetch, React, ReactDOM, Colors, ColorsConfig) {
      ReactDOM.render(
        React.createElement(Colors, ColorsConfig),
        document.getElementById(ColorsConfig.containerId)
      );
    }
  );
});
