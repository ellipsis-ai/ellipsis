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

  output(testResult) {
    return testResult.output;
  }

  containerClassNames(testResult) {
    if (testResult.isPass) {
      return "border-green";
    } else {
      return "border-pink";
    }
  }

  heading() {
    if (this.props.testResult) {
      return (
        <div className="display-inline-block">
          <span>The latest run of this test </span>
          <span className="type-italic">{this.props.testResult.isPass ? "passed" : "failed"}</span>
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
          <div className="ptxl columns columns-elastic mobile-columns-float">
            <div className="column column-expand">
              <SectionHeading number={this.props.sectionNumber}>
                <span className="mrm">{this.heading()}</span>
              </SectionHeading>

              {this.renderOutput()}
            </div>
          </div>
        </div>

      </div>
    );
  }
}

export default TestOutput;
