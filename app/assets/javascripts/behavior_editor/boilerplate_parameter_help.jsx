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
        heading="Available functions and properties"
        onCollapseClick={this.props.onCollapseClick}
      >
        <p>
          <span>The function will automatically receive the <code className="type-bold">ellipsis</code> object, which contains </span>
          <span>important methods and properties.</span>
        </p>

        <ul className="list-space-l">
          <li>
            <div>
              <span>Call <code className="type-bold">ellipsis.success</code> to end the function and include text </span>
              <span>or data for the response. You can pass a string, an object, or an array to use in the response.</span>

              <div className="box-code-example mvs">
                {'ellipsis.success("The answer is: " + answer);'}
              </div>
              <div className="box-code-example mvs">
                {"ellipsis.success({ firstName: 'Abraham', lastName: 'Lincoln' });"}
              </div>
              <div className="box-code-example mvs">
                {"ellipsis.success(['Mercury', 'Venus', 'Earth', 'Mars', 'Jupiter', 'Saturn', 'Neptune', 'Uranus']);"}
              </div>
            </div>
          </li>

          <li>
            <div>
              <span>Call <code className="type-bold">ellipsis.error</code> to end the function with an </span>
              <span>error message instead of the normal response. You must specify a string.</span>

              <div className="box-code-example mvs">
                {'ellipsis.error("There was a problem with your request.");'}
              </div>
            </div>
          </li>

          <li>
            <div>
              <span>Call <code className="type-bold">ellipsis.noResponse()</code> to end the function without </span>
              <span>sending any response.</span>
            </div>
          </li>

          <li>
            <div>
              <span>The <code className="type-bold">ellipsis.env</code> property contains any pre-configured environment </span>
              <span>variables.</span>
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
