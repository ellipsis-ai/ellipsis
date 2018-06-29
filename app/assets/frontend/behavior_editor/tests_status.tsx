import * as React from 'react';
import SVGCheckmark from '../svg/checkmark';
import SVGInfo from '../svg/info';
import SVGWarning from '../svg/warning';
import autobind from '../lib/autobind';
import BehaviorTestResult from "../models/behavior_test_result";

type Props = {
  isRunning: boolean,
  testResults?: Array<BehaviorTestResult>
}

class TestsStatus extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  isRunning() {
    return this.props.isRunning;
  }

  areResultsPresentAnd(predicate: (results: Array<BehaviorTestResult>) => boolean): boolean {
    return Boolean(this.props.testResults && this.props.testResults.length > 0 && predicate(this.props.testResults));
  }

  isAllPass() {
    return this.areResultsPresentAnd(r => r.every(ea => ea.isPass));
  }

  areFailures() {
    return this.areResultsPresentAnd(r => !!r.find(ea => !ea.isPass));
  }

  render() {
    if (this.isRunning()) {
      return (
        <span className="pll pulse type-disabled">running…</span>
      );
    } else if (this.isAllPass()) {
      return (
        <div className="pll display-inline-block fade-in">
          <span className="display-inline-block height-l mrs align-m type-green"><SVGCheckmark label="All tests pass"/></span>
          <span>all pass</span>
        </div>
      );
    } else if (this.areFailures()) {
      return (
        <div className="pll display-inline-block fade-in">
          <span className="display-inline-block align-m height-l type-yellow mrs"><SVGInfo label="There are some test failures" /></span>
          <span>failures</span>
        </div>
      );
    } else {
      return null;
    }
  }
}

export default TestsStatus;

