import * as React from 'react';
import * as ReactMarkdown from 'react-markdown';
import autobind from '../lib/autobind';
import {DataRequest} from "../lib/data_request";
import Formatter, {Timestamp} from "../lib/formatter";
import Collapsible from "../shared_ui/collapsible";
import * as moment from 'moment';
import User, {UserJson} from "../models/user";
import {PageRequiredProps} from "../shared_ui/page";
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

export interface Listener {
  id: string
  channelId: string
  channelName: Option<string>
}

type ResultsData = {
  results: ResultJson[]
}

type Props = PageRequiredProps & {
  csrfToken: string
  listener: Listener
}

interface ResultDetails {
  readonly hasRendered: boolean
  readonly isSending: boolean
  readonly hasSentPermalink: Option<string>
}

interface ResultMap {
  [id: string]: Option<ResultDetails>
}

interface State {
  lastResultTime: Option<Timestamp>
  results: Result[]
  resultDetails: ResultMap
}

class Copilot extends React.Component<Props, State> {
  resultsTimer: number | undefined;

  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = {
      lastResultTime: null,
      results: [],
      resultDetails: {}
    };
  }

  componentDidMount(): void {
    this.checkForResultsLater();
  }

  componentDidUpdate(): void {
    const resultDetails: ResultMap = {};
    this.state.results.forEach((result) => {
      if (!this.state.resultDetails[result.id]) {
        resultDetails[result.id] = {
          hasRendered: true,
          isSending: false,
          hasSentPermalink: null
        };
      }
    });
    if (Object.keys(resultDetails).length > 0) {
      this.setState({
        resultDetails: Object.assign({}, this.state.resultDetails, resultDetails)
      });
    }
  }

  hasRendered(result: Result): boolean {
    const details = this.state.resultDetails[result.id];
    return Boolean(details && details.hasRendered);
  }

  isSendingResult(result: Result): boolean {
    const details = this.state.resultDetails[result.id];
    return Boolean(details && details.isSending);
  }

  hasSentPermalink(result: Result): Option<string> {
    const details = this.state.resultDetails[result.id];
    return details ? details.hasSentPermalink : null;
  }

  checkForUpdates(): void {
    DataRequest.jsonGet(jsRoutes.controllers.CopilotController.resultsSince(this.props.listener.id, this.getLastResultTime()).url)
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

  getChannelName() {
    const channelName = this.props.listener.channelName;
    if (channelName) {
      return (
        <span>
          <span className="type-weak">#</span>
          <span>{channelName}</span>
        </span>
      );
    } else {
      return (
        <span>
          <span>unknown channel </span>
          <span className="type-disabled">(ID {this.props.listener.channelId})</span>
        </span>
      );
    }
  }

  render() {
    return (
      <div className="pvxl">
        {this.props.onRenderHeader(
          <div className="container container-narrow bg-white-translucent">
            <h5 className="mvn pvm">Copilot for {this.getChannelName()}:</h5>
          </div>
        )}
        {this.renderResults()}
        {this.props.onRenderFooter()}
      </div>
    );
  }

  renderResults() {
    const results = this.getResults();
    return (
      <div className="container container-narrow">
        {results.length > 0 ? (
          results.map(this.renderResult)
        ) : (
          <i className="type-disabled">There are no results.</i>
        )}
      </div>
    );
  }

  sendResult(result: Result) {
    const url = jsRoutes.controllers.CopilotController.sendToChannel(result.id).url;
    const beforeSendDetails: ResultMap = {};
    beforeSendDetails[result.id] = {
      hasRendered: true,
      isSending: true,
      hasSentPermalink: null
    };
    this.setState({
      resultDetails: Object.assign({}, this.state.resultDetails, beforeSendDetails)
    }, () => {
      DataRequest.jsonPost(url, {}, this.props.csrfToken).then((response) => {
        const afterSendDetails: ResultMap = {};
        afterSendDetails[result.id] = {
          hasRendered: true,
          isSending: false,
          hasSentPermalink: response
        };
        this.setState({
          resultDetails: Object.assign({}, this.state.resultDetails, afterSendDetails)
        });
      });
    });
  }

  renderResult(result: Result) {
    if (result.resultText) {
      const isSending = this.isSendingResult(result);
      const hasSentPermalink = this.hasSentPermalink(result);
      return (
        <Collapsible revealWhen={this.hasRendered(result)} key={`result-${result.id}`}>
          <div className="fade-in border mbxl bg-white">
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
            <div className="pbm phm">
              <DynamicLabelButton
                className="button-s mrm"
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
              {hasSentPermalink ? (
                <div className="align-button align-button-s height-l type-link">
                  <SVGCheckmark />
                  <a href={hasSentPermalink} target="chat" className="display-inline-block align-t mlxs">View message</a>
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
