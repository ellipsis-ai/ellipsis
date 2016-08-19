/* global ApplicationEditorConfig:false */

requirejs(['../common'], function() {
  requirejs(['react', 'react-dom', './application_editor/index'], function(React, ReactDOM, ApplicationEditor) {
    var config = ApplicationEditorConfig;
    var myApplicationEditor = React.createElement(ApplicationEditor, config);
    ReactDOM.render(myApplicationEditor, document.getElementById(config.containerId));
  });
});
