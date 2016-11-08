define(function(require) {
  var React = require('react'),
    InvocationTestResult = require('../models/behavior_invocation_result'),
    ifPresent = require('../if_present');

  return React.createClass({
    propTypes: {
      results: React.PropTypes.arrayOf(React.PropTypes.instanceOf(InvocationTestResult)).isRequired,
      resultStatus: React.PropTypes.node,
      onRenderResult: React.PropTypes.func
    },

    missingParametersResult: function(missingParamNames) {
      return (
        <div className="display-overflow-scroll border border-pink bg-white pas">
          {missingParamNames.length === 1 ? (
            <span>
              Ellipsis will ask the user for a value for the input <code className="type-bold mlxs">{missingParamNames[0]}</code>.
            </span>
          ) : (
            <span>
              <span>Ellipsis will ask the user for values for these inputs: </span>
              <code className="type-bold mlxs">{missingParamNames.join(", ")}</code>
            </span>
          )}
        </div>
      );
    },

    missingUserEnvVarsResult: function(missingUserEnvVars) {
      return (
        <div className="display-overflow-scroll border border-pink bg-white pas">
          {missingUserEnvVars.length === 1 ? (
            <span>
              If, like you, the user hasn't yet set a value for the environment variable <code className="type-bold">{missingUserEnvVars[0]}</code>, Ellipsis will ask for one.
            </span>
          ) : (
            <span>
              <span>If, like you, the user hasn't yet set values for these environment variables, Ellipsis will ask for them: </span>
              <code className="type-bold mlxs">{missingUserEnvVars.join(", ")}</code>
            </span>
          )}
        </div>
      );
    },

    componentDidUpdate: function(prevProps) {
      var resultsPane = this.refs.results;
      var hasScrolled = resultsPane.scrollHeight > resultsPane.clientHeight;
      var resultsHaveChanged = prevProps.results !== this.props.results;
      if (resultsPane && resultsHaveChanged && hasScrolled) {
        resultsPane.scrollTop = resultsPane.scrollHeight - resultsPane.clientHeight;
      }
    },

    render: function() {
      return (
        <div className="box-help">
          <div className="container phn">
            <div className="columns">
              <div className="column column-one-quarter mobile-column-full">
                <h4 className="type-weak mvs">Response log</h4>
                {this.props.resultStatus || null}
              </div>
              <div className="column column-three-quarters pll mobile-pln mobile-column-full">
                <div ref="results" className="type-s" style={{
                  maxHeight: "12rem",
                  overflow: "auto"
                }}>
                  {this.props.results.map(this.renderResult)}
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    },

    renderResult: function(result, index) {
      return (
        <div
          key={`invocationTestResult${index}`}
          className={
            "mbxs " +
            (index + 1 === this.props.results.length ? "" : "opacity-50")
          }
        >
          {this.props.onRenderResult ?
            this.props.onRenderResult(result, index) :
            this.defaultResultRenderer(result, index)
          }
        </div>
      );
    },

    defaultResultRenderer: function(result) {
      return (
        <div>
          {ifPresent(result.response, (response) => (
            <div className="display-overflow-scroll border border-green pas bg-white">
              <pre>{response}</pre>
            </div>
          ))}
          {ifPresent(result.missingUserEnvVars, this.missingUserEnvVarsResult)}
          {ifPresent(result.missingParamNames, this.missingParametersResult)}
        </div>
      );
    }
  });
});
