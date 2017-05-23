requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './application_editor/index', 'config/oauth2application/edit'],
  function(Core, Fetch, React, ReactDOM, ApplicationEditor, ApplicationEditorConfig) {
    ReactDOM.render(
      React.createElement(ApplicationEditor, ApplicationEditorConfig),
      document.getElementById(ApplicationEditorConfig.containerId)
    );
  });
});
