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
import SVGSpeechBubble from "../svg/speech_bubble";

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
  readonly hasSentPermalink?: string
  readonly sendError?: string
}

interface ResultMap {
  [id: string]: Option<ResultDetails>
}

interface State {
  loadingResults: boolean
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
      resultDetails: {},
      loadingResults: true
    };
  }

  componentDidMount(): void {
    this.checkForResultsLater(0);
  }

  componentDidUpdate(): void {
    const resultDetails: ResultMap = {};
    this.state.results.forEach((result) => {
      if (!this.state.resultDetails[result.id]) {
        resultDetails[result.id] = {
          hasRendered: true,
          isSending: false
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

  getSendError(result: Result): Option<string> {
    const details = this.state.resultDetails[result.id];
    return details ? details.sendError : null;
  }

  checkForUpdates(): void {
    DataRequest.jsonGet(jsRoutes.controllers.CopilotController.resultsSince(this.props.listener.id, this.getLastResultTime()).url)
      .then((json: ResultsData) => {
        const results = json.results.map((ea) => Object.assign({}, ea, {
          maybeUserData: ea.maybeUserData ? User.fromJson(ea.maybeUserData) : null
        }));
        this.setState({
          results: results,
          loadingResults: false
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
          <span className="color-gray-light">#</span>
          <span>{channelName}</span>
        </span>
      );
    } else {
      return (
        <span>
          <span>unknown channel </span>
          <span className="color-gray-medium">(ID {this.props.listener.channelId})</span>
        </span>
      );
    }
  }

  render() {
    const results = this.getResults();
    return (
      <div className="">
        {this.props.onRenderHeader(
          <div className="container container-narrow bg-black type-white align-c">
            <div className="mvn pvm type-label">Copilot for {this.getChannelName()}</div>
          </div>
        )}
        <div className="">
          {this.renderResults(results)}
        </div>
        {this.props.onRenderFooter()}
      </div>
    );
  }

  renderResults(results: Array<Result>) {
    if (results.length > 0) {
      return results.map(this.renderResult);
    } else if (this.state.loadingResults) {
      return (
        <div className="container container-narrow pvxl pulse type-italic type-disabled">Loading…</div>
      );
    } else {
      return (
        <div className="container container-narrow pvxl type-italic type-disabled">There are no copilot results for this channel.</div>
      );
    }
  }

  sendResult(result: Result) {
    const url = jsRoutes.controllers.CopilotController.sendToChannel(result.id).url;
    const beforeSendDetails: ResultMap = {};
    beforeSendDetails[result.id] = {
      hasRendered: true,
      isSending: true
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
      }).catch((err) => {
        const afterSendDetails: ResultMap = {};
        afterSendDetails[result.id] = {
          hasRendered: true,
          isSending: false,
          sendError: "An error occurred while trying to send this result"
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
      const sendError = this.getSendError(result);
      return (
        <Collapsible revealWhen={this.hasRendered(result)} key={`result-${result.id}`}>
          <div className="border-bottom pbs">
            <div className="pam bg-light type-s type-weak mtm mhm">
              <div>
                <b>{result.maybeUserData ? result.maybeUserData.formattedFullNameOrUserName() : "(Unknown user)"}</b>
                <span> · {Formatter.formatTimestampRelativeCalendar(result.createdAt)}</span>
              </div>
              <div className="border-left-thick border-gray pls">{result.messageText}</div>
            </div>
            <div className="ptm phm">
              <ReactMarkdown source={result.resultText} />
            </div>
            <div className="pbs columns columns-elastic">
              <div className="column column-shrink plxs">
                <DynamicLabelButton
                  className="button-subtle button-symbol"
                  onClick={this.sendResult.bind(this, result)}
                  disabledWhen={isSending}
                  labels={[{
                    text: (
                      <SVGSpeechBubble label={"Send to chat"} />
                    ),
                    displayWhen: !isSending
                  }, {
                    text: (
                      <SVGSpeechBubble label={"Sending…"} />
                    ),
                    displayWhen: isSending
                  }]}
                />
              </div>
              <div className="column column-expand align-r prm">
                {hasSentPermalink ? (
                  <div className="align-button height-xl type-s type-link">
                    <SVGCheckmark />
                    <a href={hasSentPermalink} target="chat" className="display-inline-block align-t mlxs">View message</a>
                  </div>
                ) : null}
                {sendError ? (
                  <div className="align-button align-button-s type-pink type-bold type-italic type-s">{sendError}</div>
                ) : null}
              </div>
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
