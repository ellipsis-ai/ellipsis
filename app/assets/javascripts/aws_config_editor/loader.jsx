requirejs(['common'], function() {
  requirejs(['core-js', 'whatwg-fetch', 'react', 'react-dom', './aws_config_editor/index', 'config/awsconfig/edit'],
    function(Core, Fetch, React, ReactDOM, ConfigEditor, ConfigEditorConfig) {
      ReactDOM.render(
        React.createElement(ConfigEditor, ConfigEditorConfig),
        document.getElementById(ConfigEditorConfig.containerId)
      );
    });
});
