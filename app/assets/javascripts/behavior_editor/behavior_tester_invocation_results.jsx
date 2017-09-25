define(function(require) {
  var React = require('react'),
    InvocationTestResult = require('../models/behavior_invocation_result'),
    BehaviorTesterInvocationResult = require('./behavior_tester_invocation_result');

  const BehaviorTesterInvocationResults = React.createClass({
    propTypes: {
      results: React.PropTypes.arrayOf(React.PropTypes.instanceOf(InvocationTestResult)).isRequired,
      resultStatus: React.PropTypes.node,
      onRenderResult: React.PropTypes.func
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
              <div className="column column-page-sidebar">
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
      const isMostRecentResult = index + 1 === this.props.results.length;
      return (
        <div
          key={`invocationTestResult${index}`}
          className={
            "mbxs " +
            (isMostRecentResult ? "" : "opacity-50")
          }
        >
          {this.props.onRenderResult ? (
            this.props.onRenderResult(result)
          ) : (
            <BehaviorTesterInvocationResult result={result} />
          )}
        </div>
      );
    }
  });

  return BehaviorTesterInvocationResults;
});
