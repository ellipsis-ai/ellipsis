import * as React from 'react';
import InvocationTestResult from '../models/behavior_invocation_result';
import BehaviorTesterInvocationResult from './behavior_tester_invocation_result';
import autobind from "../lib/autobind";

interface Props {
  results: Array<InvocationTestResult>,
  resultStatus?: any,
  onRenderResult?: (result: InvocationTestResult) => any
}

class BehaviorTesterInvocationResults extends React.Component<Props> {
  results: Option<HTMLDivElement>;

  constructor(props: Props) {
    super(props);
    autobind(this);
    this.results = null;
  }

  componentDidUpdate(prevProps: Props): void {
      var resultsPane = this.results;
      var hasScrolled = resultsPane && resultsPane.scrollHeight > resultsPane.clientHeight;
      var resultsHaveChanged = prevProps.results !== this.props.results;
      if (resultsPane && resultsHaveChanged && hasScrolled) {
        resultsPane.scrollTop = resultsPane.scrollHeight - resultsPane.clientHeight;
      }
    }

    render() {
      return (
        <div className="box-help">
          <div className="container phn">
            <div className="columns">
              <div className="column column-page-sidebar">
                <h4 className="type-weak mvs">Response log</h4>
                {this.props.resultStatus || null}
              </div>
              <div className="column column-three-quarters pll mobile-pln mobile-column-full">
                <div ref={(el) => this.results = el} className="type-s" style={{
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
    }

    renderResult(result: InvocationTestResult, index: number) {
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
}

export default BehaviorTesterInvocationResults;

