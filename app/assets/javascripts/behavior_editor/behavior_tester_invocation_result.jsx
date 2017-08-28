define(function(require) {
  const React = require('react'),
    InvocationTestResult = require('../models/behavior_invocation_result');

  class BehaviorTesterInvocationResult extends React.Component {
    containerClassNames() {
      const result = this.props.result;
      if (result.wasSuccessful()) {
        return "border-green";
      } else if (result.wasNoResponse()) {
        return "";
      } else {
        return "border-pink";
      }
    }

    missingSimpleTokens() {
      const missingSimpleTokens = this.props.result.missingSimpleTokens;
      return (
        <div>
          {missingSimpleTokens.length === 1 ? (
            <span>
              If, like you, the user hasn’t yet supplied a token for the <code className="type-bold">{missingSimpleTokens[0]}</code> API, Ellipsis will ask for one.
            </span>
          ) : (
            <span>
              <span>If, like you, the user hasn’t yet supplied tokens for these APIs, Ellipsis will ask for them: </span>
              <code className="type-bold mlxs">{missingSimpleTokens.join(", ")}</code>
            </span>
          )}
        </div>
      );
    }

    missingInputs() {
      const missingInputNames = this.props.result.missingInputNames;
      return (
        <div>
          {missingInputNames.length === 1 ? (
            <span>
              Ellipsis will ask the user for a value for the input <code className="type-bold mlxs">{missingInputNames[0]}</code>.
            </span>
          ) : (
            <span>
              <span>Ellipsis will ask the user for values for these inputs: </span>
              <code className="type-bold mlxs">{missingInputNames.join(", ")}</code>
            </span>
          )}
        </div>
      );
    }

    missingUserEnvVars() {
      const missingUserEnvVars = this.props.result.missingUserEnvVars;
      return (
        <div>
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
    }

    renderText() {
      const result = this.props.result;
      if (result.responseText) {
        return (
          <pre>{result.responseText}</pre>
        );
      } else if (result.wasNoResponse()) {
        return (
          <i>The action completed, but sent no response.</i>
        );
      } else {
        return (
          <div>
            {result.missingSimpleTokens.length > 0 ? this.missingSimpleTokens() : null}
            {result.missingUserEnvVars.length > 0 ? this.missingUserEnvVars() : null}
            {result.missingInputNames.length > 0 ? this.missingInputs() : null}
          </div>
        );
      }
    }

    render() {
      return (
        <div
          className={
            `display-overflow-scroll border pas bg-white ${
              this.containerClassNames()
            }`}
        >
          {this.renderText()}
        </div>
      );
    }
  }

  BehaviorTesterInvocationResult.propTypes = {
    result: React.PropTypes.instanceOf(InvocationTestResult).isRequired
  };

  return BehaviorTesterInvocationResult;
});
