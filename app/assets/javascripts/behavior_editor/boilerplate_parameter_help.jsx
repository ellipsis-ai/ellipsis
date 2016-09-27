define(function(require) {
var React = require('react'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  HelpPanel = require('../help/panel'),
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
        heading="Available methods and properties"
        onCollapseClick={this.props.onCollapseClick}
      >
        <p>
          <span>The function will automatically receive the <code className="type-bold">ellipsis</code> object, which contains </span>
          <span>important methods and properties.</span>
        </p>

        <div className="columns columns-elastic">
          <div className="column-group">
            <div className="column-row">
              <div className="column column-shrink pbl prxl"><pre>{"ellipsis \u007B"}</pre></div>
              <div className="column column-expand pbl"></div>
            </div>

            <div className="column-row">
              <div className="column column-shrink pbl prxl"><pre>  success(successResult)</pre></div>
              <div className="column column-expand pbl">
                <span>Ends the function, passing <span className="type-monospace type-bold">successResult</span> to </span>
                <span>the response template and then displaying it to the user. </span>
                <span><span className="type-monospace type-bold">successResult</span> can be a string, array, </span>
                <span>or object. </span>
                <button type="button" className="button-raw">Examples</button>
              </div>
            </div>

            <div className="column-row">
              <div className="column column-shrink pbl prxl"><pre>  error(message)</pre></div>
              <div className="column column-expand pbl">
                <span>Ends the function by showing an error message to the user. </span>
                <span><span className="type-monospace type-bold">message</span> should be a string. </span>
                <button type="button" className="button-raw">Examples</button>
              </div>
            </div>

            <div className="column-row">
              <div className="column column-shrink pbl prxl"><pre>  noResponse()</pre></div>
              <div className="column column-expand pbl">
                <span>Ends the function without sending a response.</span>
              </div>
            </div>

            <div className="column-row">
              <div className="column column-shrink pbl prxl"><pre>  env</pre></div>
              <div className="column column-expand pbl">
                <span>Contains any configured <b>environment variables</b> as properties, accessible by name. </span>
                <button type="button" className="button-raw">Show list</button>
              </div>
            </div>

            <div className="column-row">
              <div className="column column-shrink pbl prxl"><pre>  accessTokens</pre></div>
              <div className="column column-expand pbl">
                <span>Contains any <b>API access tokens</b> added to the behavior. </span>
                <button type="button" className="button-raw">Show list</button>
              </div>
            </div>

            <div className="column-row">
              <div className="column column-shrink pbl prxl"><pre>  AWS</pre></div>
              <div className="column column-expand pbl">
                <span>Contains properties and methods of the <b>Amazon Web Services</b> (AWS) SDK. </span>
                <button type="button" className="button-raw">Help</button>
              </div>
            </div>

            <div className="column-row">
              <div className="column column-shrink pbl prxl"><pre>{"\u007D"}</pre></div>
              <div className="column column-expand pbl"></div>
            </div>
          </div>
        </div>

      </HelpPanel>
    );
  }
});

});
