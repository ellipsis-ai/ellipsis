import * as React from 'react';
import * as ReactMarkdown from 'react-markdown';
import autobind from '../lib/autobind';
import {DataRequest} from "../lib/data_request";
import Formatter, {Timestamp} from "../lib/formatter";
import Collapsible from "../shared_ui/collapsible";
import * as moment from 'moment';
import User, {UserJson} from "../models/user";

interface ResultJson {
  id: string
  messageText: string
  resultType: string
  resultText: string
  createdAt: Timestamp
  maybeChannel: Option<string>
  maybeUserIdForContext: Option<string>
  maybeUserData: Option<UserJson>
}

interface Result extends ResultJson {
  maybeUserData: Option<User>
}

type ResultsData = {
  results: ResultJson[]
}

interface Props {

}

interface HasRenderedMap {
  [id: string]: Option<boolean>
}

interface State {
  lastResultTime: Option<Timestamp>,
  results: Result[]
  hasRendered: HasRenderedMap
}

class Copilot extends React.Component<Props, State> {
  resultsTimer: number | undefined;

  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = {
      lastResultTime: null,
      results: [],
      hasRendered: {}
    };
  }

  componentDidMount(): void {
    this.checkForResultsLater();
  }

  componentDidUpdate(): void {
    const hasRendered: HasRenderedMap = {};
    this.state.results.forEach((result) => {
      if (!this.state.hasRendered[result.id]) {
        hasRendered[result.id] = true;
      }
    });
    if (Object.keys(hasRendered).length > 0) {
      this.setState({
        hasRendered: Object.assign({}, this.state.hasRendered, hasRendered)
      });
    }
  }

  hasRendered(result: Result): boolean {
    return Boolean(this.state.hasRendered[result.id]);
  }

  checkForUpdates(): void {
    DataRequest.jsonGet(jsRoutes.controllers.CopilotController.resultsSince(this.getLastResultTime()).url)
      .then((json: ResultsData) => {
        const results = json.results.map((ea) => Object.assign({}, ea, {
          maybeUserData: ea.maybeUserData ? User.fromJson(ea.maybeUserData) : null
        }));
        this.setState({
          results: results
        });
        this.checkForResultsLater();
      });
  }

  getResults(): Result[] {
    return this.state.results.sort((a, b) => {
      if (moment(a.createdAt).isBefore(b.createdAt)) {
        return 1;
      } else {
        return -1;
      }
    });
  }

  getLastResultTime(): string {
    const timestamp = this.state.lastResultTime || Date.now();
    return Formatter.formatTimestampRelativeCalendar(timestamp);
  }

  checkForResultsLater(overrideDuration?: number): void {
    clearTimeout(this.resultsTimer);
    this.resultsTimer = setTimeout(this.checkForUpdates, overrideDuration || 2000);
  }

  render() {
    return (
      <div className="container container-narrow">
        <h2>Your bot said:</h2>
        {this.getResults().map(this.renderResult)}
      </div>
    );
  }

  renderResult(result: Result) {
    if (result.resultText) {
      return (
        <Collapsible revealWhen={this.hasRendered(result)} key={`result-${result.id}`}>
          <div className="fade-in border mvl bg-white">
            <div className="border-bottom border-light pam bg-light type-s type-weak">
              <div>
                <b>{result.maybeUserData ? result.maybeUserData.formattedFullNameOrUserName() : "(Unknown user)"}</b>
                <span> · {Formatter.formatTimestampRelativeCalendar(result.createdAt)}</span>
              </div>
              <div className="border-left-thick border-gray pls">{result.messageText}</div>
            </div>
            <div className="ptm phm">
              <ReactMarkdown source={result.resultText} />
            </div>
          </div>
        </Collapsible>
      )
    } else {
      return null;
    }
  }

}

export default Copilot;
