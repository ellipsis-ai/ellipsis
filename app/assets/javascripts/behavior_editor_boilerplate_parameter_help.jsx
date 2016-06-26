define(function(require) {
var React = require('react'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  BehaviorEditorHelpPanel = require('./behavior_editor_help_panel'),
  BehaviorEditorEnvVariableList = require('./behavior_editor_env_variable_list'),
  BehaviorEditorNoEnvVariables = require('./behavior_editor_no_env_variables');

return React.createClass({
  mixins: [BehaviorEditorMixin],
  propTypes: {
    envVariableNames: React.PropTypes.arrayOf(React.PropTypes.string),
    expandEnvVariables: React.PropTypes.bool.isRequired,
    onCollapseClick: React.PropTypes.func.isRequired,
    onExpandToggle: React.PropTypes.func.isRequired
  },
  onExpandToggle: function() {
    this.refs.button.blur();
    this.props.onExpandToggle();
  },
  render: function() {
    return (
      <BehaviorEditorHelpPanel
        heading="Function parameters"
        onCollapseClick={this.props.onCollapseClick}
      >
        <p>
          <span>The function will automatically receive three parameters from Ellipsis:</span>
        </p>

        <ul>
          <li className="mbs">
            <div>
              <span>Call the <code className="type-bold">onSuccess</code> function to send a response to the user.</span>
              <span> e.g. </span>
              <code className="type-weak">{'onSuccess("It worked!");'}</code>
            </div>
          </li>

          <li className="mbs">
            <div>
              <span>Call the <code className="type-bold">onError</code> function to send an error message, e.g. </span>
              <code className="type-weak">onError("Something went wrong.");</code>
            </div>
          </li>

          <li className="mbs">
            <div>
              <span>Use the <code className="type-bold">ellipsis</code> object when required by library </span>
              <span>methods. It also contains any pre-configured environment variables </span>
              <span>in its <code className="type-bold">env</code> property.</span>
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
      </BehaviorEditorHelpPanel>
    );
  }
});

});
