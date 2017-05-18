requirejs(['../../common'], function() {
  requirejs(['config/styleguide/colors'], function(ColorsConfig) {
    requirejs(
      ['core-js', 'whatwg-fetch', 'react', 'react-dom', './styleguide/colors/index'],
      function(Core, Fetch, React, ReactDOM, Colors) {
        ReactDOM.render(
          React.createElement(Colors, ColorsConfig),
          document.getElementById(ColorsConfig.containerId)
        );
      }
    );
  });
});
