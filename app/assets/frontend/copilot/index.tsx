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
import Textarea from "../form/textarea";
import Button from "../form/button";

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
  readonly sendError?: Option<string>
  readonly text?: string
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

  revealEditorFor(result: Result, event: React.MouseEvent): void {
    if (event.shiftKey) {
      this.sendResult(result);
    } else {
      this.props.onToggleActivePanel(result.id, true);
    }
  }

  getTextFor(result: Result): string {
    const text = this.getResultDetailsPropValue(result, "text");
    return typeof text === "string" ? text : result.resultText;
  }

  setTextFor(result: Result, value: string): void {
    this.setState({
      resultDetails: this.updateDetailsFor(result, {
        text: value
      })
    });
  }

  getResultDetailsPropValue<K extends keyof ResultDetails>(result: Result, propValue: K): Option<ResultDetails[K]> {
    const details = this.state.resultDetails[result.id];
    return details ? details[propValue] : null;
  }

  hasRendered(result: Result): boolean {
    return Boolean(this.getResultDetailsPropValue(result, "hasRendered"));
  }

  isSendingResult(result: Result): boolean {
    return Boolean(this.getResultDetailsPropValue(result, "isSending"));
  }

  hasSentPermalink(result: Result): Option<string> {
    return this.getResultDetailsPropValue(result, "hasSentPermalink");
  }

  getSendError(result: Result): Option<string> {
    return this.getResultDetailsPropValue(result, "sendError");
  }

  doneEditing(): void {
    this.props.onClearActivePanel();
  }

  cancelChangesFor(result: Result): void {
    this.setTextFor(result, result.resultText);
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
    return this.state.results.filter((ea) => Boolean(ea.resultText)).sort((a, b) => {
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
          <span>Unknown channel </span>
          <span className="color-gray-medium">(ID {this.props.listener.channelId})</span>
        </span>
      );
    }
  }

  render() {
    const results = this.getResults();
    return (
      <div>
        {this.props.onRenderHeader(
          <div className="container container-narrow bg-black type-white align-c">
            <div className="mvn pvm type-label">{this.getChannelName()}</div>
          </div>
        )}
        {this.renderResults(results)}
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

  updateDetailsFor(result: Result, details: Partial<ResultDetails>): ResultMap {
    const updated: ResultMap = {};
    updated[result.id] = Object.assign({}, this.state.resultDetails[result.id], details);
    return Object.assign({}, this.state.resultDetails, updated);
  }

  sendResult(result: Result) {
    const url = jsRoutes.controllers.CopilotController.sendToChannel(result.id).url;
    this.doneEditing();
    this.setState({
      resultDetails: this.updateDetailsFor(result, {
        isSending: true,
        sendError: null
      })
    }, () => {
      const text = this.getTextFor(result);
      DataRequest.jsonPost(url, {
        text: text === result.resultText ? null : text
      }, this.props.csrfToken).then((response) => {
        this.setState({
          resultDetails: this.updateDetailsFor(result, {
            isSending: false,
            hasSentPermalink: response
          })
        });
      }).catch(() => {
        this.setState({
          resultDetails: this.updateDetailsFor(result, {
            isSending: false,
            sendError: "An error occurred while trying to send this result"
          })
        });
      });
    });
  }

  renderResult(result: Result) {
    const isSending = this.isSendingResult(result);
    const hasSentPermalink = this.hasSentPermalink(result);
    const sendError = this.getSendError(result);
    const showEditor = this.props.activePanelName === result.id;
    const editorText = this.getTextFor(result);
    const hasChanges = result.resultText !== editorText;
    return (
        <Collapsible animationDuration={0.5} revealWhen={this.hasRendered(result)} key={`result-${result.id}`}>
          <div className="position-relative border-bottom ptl pbs" onClick={this.revealEditorFor.bind(this, result)}>
            {showEditor ? (
              <div className="position-absolute position-top-left position-top-right position-z-front fade-in bg-white phl pvm" onClick={event => event.stopPropagation()}>
                <h5 className="mtn mbxs">Edit response</h5>
                <Textarea
                  className="form-input-borderless form-input-height-auto"
                  onChange={this.setTextFor.bind(this, result)}
                  value={editorText}
                  rows={4}
                  autoFocus={true}
                />
                <div className="mtm">
                  <Button className="button-s mrm" onClick={this.sendResult.bind(this, result)}>Send</Button>
                  <Button className="button-s mrm" onClick={this.doneEditing}>Cancel</Button>
                  <Button className="button-s mrm" onClick={this.cancelChangesFor.bind(this, result)} disabled={!hasChanges}>Undo changes</Button>
                </div>
              </div>
            ) : null}
            <div className="type-weak type-s mhl border-left-thick border-gray pls">
              <div>
                <b>{result.maybeUserData ? result.maybeUserData.formattedFullNameOrUserName() : "(Unknown user)"}</b>
                <span> · {Formatter.formatTimestampRelativeCalendar(result.createdAt)}</span>
              </div>
              <div>{result.messageText}</div>
            </div>
            <div className="ptm phl">
              <ReactMarkdown source={result.resultText} />
            </div>
            <div className="pbs columns columns-elastic">
              <div className="column column-shrink plxs">
                <DynamicLabelButton
                  className="button-subtle button-shrink"
                  onClick={() => {}}
                  disabledWhen={isSending}
                  hoverOnClassName={"visibility-children-visible"}
                  labels={[{
                    text: (
                      <span className="display-inline-block height-xl">
                        <SVGSpeechBubble label={"Send to chat"} />
                        <span className="mlxs display-inline-block align-t type-s visibility parent-controlled-visibility">Send to chat</span>
                      </span>
                    ),
                    displayWhen: !isSending
                  }, {
                    text: (
                      <span className="display-inline-block height-xl">
                        <SVGSpeechBubble label={"Sending…"} />
                        <span className="mlxs display-inline-block align-t type-s">Sending…</span>
                      </span>
                    ),
                    displayWhen: isSending
                  }]}
                />
              </div>
              <div className="column column-expand align-r prl">
                {hasSentPermalink ? (
                  <div className="align-button height-xl type-s type-link">
                    <SVGCheckmark />
                    <a href={hasSentPermalink} target="chat" className="display-inline-block align-t mlxs" onClick={event => event.stopPropagation()}>View message</a>
                  </div>
                ) : null}
                {sendError ? (
                  <div className="align-button align-button-s type-pink type-bold type-italic type-s">{sendError}</div>
                ) : null}
              </div>
            </div>
          </div>
        </Collapsible>
    );
  }

}

export default Copilot;
