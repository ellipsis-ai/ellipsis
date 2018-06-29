import * as React from 'react';
import SectionHeading from '../shared_ui/section_heading';
import autobind from "../lib/autobind";
import BehaviorTestResult from "../models/behavior_test_result";

interface Props {
  sectionNumber: string,
  testResult: Option<BehaviorTestResult>
}

class TestOutput extends React.Component<Props> {

  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  output(testResult: BehaviorTestResult) {
    return testResult.output;
  }

  containerClassNames(testResult: BehaviorTestResult) {
    if (testResult.isPass) {
      return "border-green";
    } else {
      return "border-pink";
    }
  }

  resultClassNames(testResult: BehaviorTestResult) {
    return testResult.isPass ? "type-green" : "type-pink";
  }

  renderTestResults() {
    const result = this.props.testResult;
    if (result) {
      return (
        <div className={this.resultClassNames(result)}>
          <span>The latest run of this test </span>
          <span className="type-bold">{result.isPass ? "passed" : "failed"}</span>
          <span>.</span>
        </div>
      )
    } else {
      return (
        <span className="pulse type-disabled">This test is currently running…</span>
      );
    }
  }

  renderOutput() {
    if (this.props.testResult) {
      return (
        <div className="position-relative">
          <div
            className={
              `display-overflow-scroll border pas bg-white ${
                this.containerClassNames(this.props.testResult)
                }`}
          >
            <pre>{this.output(this.props.testResult)}</pre>
          </div>
        </div>
      );
    } else {
      return null;
    }
  }

  render() {
    return (
      <div>

        <div className="container container-wide">
          <div className="ptxl">
            <SectionHeading number={this.props.sectionNumber}>Results</SectionHeading>
            <div className="mvl">{this.renderTestResults()}</div>

            {this.renderOutput()}
          </div>
        </div>

      </div>
    );
  }
}

export default TestOutput;
