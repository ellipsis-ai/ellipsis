define(function(require) {
var React = require('react'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  BehaviorEditorHelpButton = require('./behavior_editor_help_button'),
  BehaviorEditorEnvVariableList = require('./behavior_editor_env_variable_list'),
  BehaviorEditorNoEnvVariables = require('./behavior_editor_no_env_variables');

return React.createClass({
  displayName: 'BehaviorEditorBoilerplateParameterHelp',
  mixins: [BehaviorEditorMixin],
  onExpandToggle: function() {
    this.refs.button.blur();
    this.props.onExpandToggle();
  },
  render: function() {
    return (
      <div className="pvs">
        <div className="position-relative bg-blue-lighter border border-emphasis-left border-blue type-s pal">

          <div className="position-absolute position-top-right ptxs prxs">
            <BehaviorEditorHelpButton onClick={this.props.onCollapseClick} toggled={true} inline={true} />
          </div>

          <p>
            <span>In addition to any parameters you add, the function will receive three parameters from Ellipsis. </span>
            <span><strong>You must use <code>onSuccess</code> to show a response to the user.</strong> </span>
            <span>(Using <code className="type-weak">return</code> won’t work.)</span>
          </p>

          <ul>
            <li className="mbs">
              <div>
                <span><code className="type-bold">onSuccess</code> is a callback function used to send the desired response, e.g. </span>
                <code className="type-weak">{'onSuccess("It worked!");'}</code>
              </div>
            </li>

            <li className="mbs">
              <div>
                <span><code className="type-bold">onError</code> is a callback function used to send an error message, e.g. </span>
                <code className="type-weak">onError("Something went wrong.");</code>
              </div>
            </li>

            <li className="mbs">
              <div>
                <span>The <code className="type-bold">ellipsis</code> object is required when using the </span>
                <span>default storage library, and it also contains any pre-configured environment variables </span>
                <span>in the <code className="type-bold">env</code> property.</span>
              </div>
            </li>
          </ul>

          <button type="button"
            ref="button"
            className="button-none pan mbxs display-limit-width"
            onClick={this.onExpandToggle}
          >
            {
              this.props.envVariableNames.length > 0 ? (
                <BehaviorEditorEnvVariableList
                  envVariableNames={this.props.envVariableNames}
                  expandEnvVariables={this.props.expandEnvVariables}
                />
              ) : (
                <BehaviorEditorNoEnvVariables />
              )
            }
          </button>
        </div>
      </div>
    );
  }
});

});
