import * as React from 'react';
import autobind from '../lib/autobind';
import {DataRequest} from "../lib/data_request";

type Result = {
  resultText: string,
  createdAt: string
}

type ResultsData = {
  results: Result[]
}

type Props = {

}

type State = {
  lastResultTime: string | undefined,
  results: Result[]
}

class Copilot extends React.Component<Props, State> {
  resultsTimer: number | undefined;

  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  componentDidMount(): void {
    this.checkForResultsLater();
  }

  checkForUpdates(): void {
    DataRequest.jsonGet(jsRoutes.controllers.CopilotController.resultsSince(this.getLastResultTime()).url)
      .then((json: ResultsData) => {
        this.setState({
          results: json.results
        });
        this.checkForResultsLater();
      });
  }

  getResults(): Result[] {
    return this.state ? (this.state.results || []) : [];
  }

  getLastResultTime(): string {
    return (this.state && this.state.lastResultTime) ?
      this.state.lastResultTime :
      new Date((new Date().getTime() - 1000 * 60 * 60 * 24)).toISOString();
  }

  checkForResultsLater(overrideDuration?: number): void {
    clearTimeout(this.resultsTimer);
    this.resultsTimer = setTimeout(this.checkForUpdates, overrideDuration || 2000);
  }

  render() {
    return (
      <div>
        <h2>Your bot said:</h2>
        {this.getResults().map(this.renderResult)}
      </div>
    );
  }

  renderResult(result: Result) {
    return (
      <div>
        <span>{result.resultText} at {new Date(Date.parse(result.createdAt)).toISOString()}</span>
      </div>
    )
  }

}

export default Copilot;
