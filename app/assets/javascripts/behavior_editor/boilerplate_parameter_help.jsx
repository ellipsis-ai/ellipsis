define(function(require) {
var React = require('react'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  HelpPanel = require('./help_panel'),
  EnvVariableList = require('./env_variable_list'),
  NoEnvVariables = require('./no_env_variables');

return React.createClass({
  mixins: [BehaviorEditorMixin],
  propTypes: {
    envVariableNames: React.PropTypes.arrayOf(React.PropTypes.string),
    expandEnvVariables: React.PropTypes.bool.isRequired,
    onAddNew: React.PropTypes.func.isRequired,
    onCollapseClick: React.PropTypes.func.isRequired,
    onExpandToggle: React.PropTypes.func.isRequired
  },
  onExpandToggle: function() {
    this.refs.button.blur();
    this.props.onExpandToggle();
  },
  render: function() {
    return (
      <HelpPanel
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

        <div className="mbs">
          <button type="button"
            ref="button"
            className="button-none pan display-limit-width"
            onClick={this.onExpandToggle}
          >
            <h5 className="display-inline-block">
              <span
                className="display-inline-block"
                style={{ width: '0.8em' }}
              >{this.props.expandEnvVariables ? "▾" : "▸"}</span>
              <span> Current environment variables</span>
            </h5>
            <span className="link mls">
              {this.props.expandEnvVariables ? "Collapse" : "Expand"}
            </span>
          </button>

          {
            this.props.envVariableNames.length > 0 ? (
              <EnvVariableList
                envVariableNames={this.props.envVariableNames}
                expandEnvVariables={this.props.expandEnvVariables}
              />
            ) : (
              <NoEnvVariables />
            )
          }
        </div>

        <div className="mbxs">
          <button type="button"
            className="button-s"
            onClick={this.props.onAddNew}
          >
            New environment variable
          </button>
        </div>
      </HelpPanel>
    );
  }
});

});
