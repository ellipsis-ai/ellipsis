import * as React from 'react';
import * as ReactMarkdown from 'react-markdown';
import autobind from '../lib/autobind';
import {DataRequest} from "../lib/data_request";
import Formatter, {Timestamp} from "../lib/formatter";
import Collapsible from "../shared_ui/collapsible";
import * as moment from 'moment';
import User, {UserJson} from "../models/user";
import {PageRequiredProps} from "../shared_ui/page";
import Button from "../form/button";
import DynamicLabelButton from "../form/dynamic_label_button";
import SVGCheckmark from "../svg/checkmark";

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

type Props = PageRequiredProps & {
  csrfToken: string
}

interface ResultMap {
  [id: string]: Option<boolean>
}

interface State {
  lastResultTime: Option<Timestamp>
  results: Result[]
  hasRendered: ResultMap
  sendingResults: ResultMap
  sentResults: ResultMap
}

class Copilot extends React.Component<Props, State> {
  resultsTimer: number | undefined;

  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = {
      lastResultTime: null,
      results: [],
      hasRendered: {},
      sendingResults: {},
      sentResults: {}
    };
  }

  componentDidMount(): void {
    this.checkForResultsLater();
  }

  componentDidUpdate(): void {
    const hasRendered: ResultMap = {};
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
        <h3>Copilot results:</h3>
        {this.renderResults()}
        {this.props.onRenderFooter()}
      </div>
    );
  }

  renderResults() {
    const results = this.getResults();
    if (results.length > 0) {
      return results.map(this.renderResult)
    } else {
      return (
        <div className="type-disabled">
          <i>There are no results.</i>
        </div>
      )
    }
  }

  isSendingResult(result: Result): boolean {
    return Boolean(this.state.sendingResults[result.id]);
  }

  hasSentResult(result: Result): boolean {
    return Boolean(this.state.sentResults[result.id]);
  }

  sendResult(result: Result) {
    const url = jsRoutes.controllers.CopilotController.sendToChannel(result.id).url;
    const isSending: ResultMap = {};
    const isSent: ResultMap = {};
    isSending[result.id] = true;
    isSent[result.id] = false;
    this.setState({
      sendingResults: Object.assign({}, this.state.sendingResults, isSending),
      sentResults: Object.assign({}, this.state.sentResults, isSent)
    }, () => {
      DataRequest.jsonPost(url, {}, this.props.csrfToken).then((response) => {
        isSending[result.id] = false;
        isSent[result.id] = true;
        this.setState({
          sendingResults: Object.assign({}, this.state.sendingResults, isSending),
          sentResults: Object.assign({}, this.state.sentResults, isSent)
        });
      });
    });
  }

  renderResult(result: Result) {
    if (result.resultText) {
      const isSending = this.isSendingResult(result);
      const hasSent = this.hasSentResult(result);
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
            <div className="border-top border-light pam bg-lightest">
              <DynamicLabelButton
                className="button-s mrs"
                onClick={this.sendResult.bind(this, result)}
                disabledWhen={isSending}
                labels={[{
                  text: "Send to chat",
                  displayWhen: !isSending
                }, {
                  text: "Sending…",
                  displayWhen: isSending
                }]}
              />
              {hasSent ? (
                <div className="align-button align-button-s height-l type-green">
                  <SVGCheckmark />
                  <div className="display-inline-block align-t mlxs">Sent</div>
                </div>
              ) : null}
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
