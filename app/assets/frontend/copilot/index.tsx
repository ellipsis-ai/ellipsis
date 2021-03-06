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
  createdAt: string
  maybeChannel: Option<string>
  maybeUserIdForContext: Option<string>
  maybeUserData: Option<UserJson>
}

interface Result extends ResultJson {
  maybeUserData: Option<User>
}

export interface Listener {
  id: string
  channel: string
  channelName: Option<string>
  behaviorGroupName: Option<string>
  behaviorGroupIcon: Option<string>
  medium: string
  mediumDescription: Option<string>
}

type ResultsData = {
  results: ResultJson[]
}

type Props = PageRequiredProps & {
  csrfToken: string
  teamName: string
  listener: Listener
  user: User
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
  lastUpdated: Option<Timestamp>
  results: Result[]
  resultDetails: ResultMap
  errorLoadingResults: boolean
}

class Copilot extends React.Component<Props, State> {
  resultsTimer: number | undefined;

  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = {
      lastUpdated: null,
      results: [],
      resultDetails: {},
      loadingResults: true,
      errorLoadingResults: false
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
    this.setState({
      loadingResults: true,
      errorLoadingResults: false
    });
    DataRequest.jsonGet(jsRoutes.controllers.CopilotController.resultsSince(this.props.listener.id, this.getMostRecentResultTime()).url)
      .then((json: ResultsData) => {
        const results = json.results.map((ea) => Object.assign({}, ea, {
          maybeUserData: ea.maybeUserData ? User.fromJson(ea.maybeUserData) : null
        }));
        const oldResults = this.state.results;
        this.setState({
          results: this.state.results.concat(results.filter((newResult) => !oldResults.some((oldResult) => oldResult.id === newResult.id))),
          loadingResults: false,
          lastUpdated: (new Date()).toISOString()
        });
        this.checkForResultsLater();
      }).catch(() => {
        this.setState({
          loadingResults: false,
          errorLoadingResults: true
        });
      });
  }

  getDisplayableResultsSortedDescending(): Result[] {
    return this.state.results.filter((ea) => Boolean(ea.resultText)).sort((a, b) => {
      if (moment(a.createdAt).isBefore(b.createdAt)) {
        return 1;
      } else {
        return -1;
      }
    });
  }

  getMostRecentResultTime(): Option<string> {
    const mostRecentResult = this.getDisplayableResultsSortedDescending()[0];
    return mostRecentResult ? mostRecentResult.createdAt : null;
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
          <span className="color-gray-medium">#</span>
          <span>{channelName}</span>
        </span>
      );
    } else {
      return (
        <span>
          <span>Unknown channel </span>
          <span className="color-gray-medium">(ID {this.props.listener.channel})</span>
        </span>
      );
    }
  }

  renderStatus() {
    if (this.state.loadingResults && !this.state.lastUpdated) {
      return (
        <div className="pulse">Updating…</div>
      );
    } else if (this.state.lastUpdated) {
      return (
        <div className="columns columns-elastic height-xl">
          <div className="column column-expand">
            Last updated: {Formatter.formatTimestampShort(this.state.lastUpdated)}
          </div>
          <div className="column column-shrink display-nowrap">
            {this.state.loadingResults ? (
              <span className="pulse color-green-medium">●</span>
            ) : null}
            {this.state.errorLoadingResults ? (
              <div>
                <Button className="button-s button-subtle button-shrink" onClick={this.checkForUpdates}>
                  <span className="color-pink-medium type-label">Retry</span>
                </Button>
                <span className="color-pink-medium">●</span>
              </div>
            ) : null}
          </div>
        </div>
      );
    } else {
      return (
        <div>Error during last update</div>
      );
    }
  }

  render() {
    const results = this.getDisplayableResultsSortedDescending();
    return (
      <div className="max-width-40">
        {this.props.onRenderHeader(
          <div className="phl bg-black type-white pvs width max-width-40">
            <div className="type-xs">
              <span className="type-label">
                <span className="color-gray-medium">Co-pilot for </span>
                <span>{this.props.user.formattedFullNameOrUserName()}</span>
              </span>
            </div>
            <div className="type-xs">
              <span className="display-inline-block type-label">
                <span className="color-gray-medium">Listening in </span>
                <span>{this.props.listener.mediumDescription || this.props.listener.medium}</span>
              </span>
              <span className="color-gray-medium display-inline-block mhs">·</span>
              <span className="display-inline-block type-label">
                <span className="color-gray-medium">Workspace: </span>
                <span>{this.props.teamName}</span>
              </span>
              <span className="color-gray-medium display-inline-block mhs">·</span>
              <span className="display-inline-block type-label">
                <span className="color-gray-medium">Channel: </span>
                {this.getChannelName()}
              </span>
            </div>
          </div>
        )}
        <div className="bg-light phl pvs type-s type-weak">
          {this.renderStatus()}
        </div>
        {this.renderResults(results)}
        {this.props.onRenderFooter()}
      </div>
    );
  }

  renderResults(results: Array<Result>) {
    if (results.length > 0) {
      return results.map(this.renderResult);
    } else if (this.state.loadingResults && !this.state.lastUpdated) {
      return null;
    } else {
      return (
        <div className="phl bg-white pvxl type-italic type-disabled">There are no copilot results for this channel.</div>
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

  renderResult(result: Result, index: number) {
    const isSending = this.isSendingResult(result);
    const hasSentPermalink = this.hasSentPermalink(result);
    const sendError = this.getSendError(result);
    const showEditor = this.props.activePanelName === result.id;
    const editorText = this.getTextFor(result);
    const hasChanges = result.resultText !== editorText;
    return (
        <Collapsible animationDuration={0.5} revealWhen={this.hasRendered(result)} key={`result-${result.id}`}>
          <div className="bg-white position-relative border-top ptl pbs" onClick={this.revealEditorFor.bind(this, result)}>
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
            <div className="type-weak type-s mll border-left-thick border-gray pls">
              <div className="columns columns-elastic">
                <div className="column column-expand">
                  <b>{result.maybeUserData ? result.maybeUserData.formattedFullNameOrUserName() : "(Unknown user)"}</b>
                  <span> · {Formatter.formatTimestampRelativeCalendar(result.createdAt)}</span>
                </div>
                <div className="column column-shrink">
                  {index === 0 ? (
                    <span className="type-label border-radius-left bg-black type-white phxs mls">Latest</span>
                  ) : null}
                </div>
              </div>
              <div>{result.messageText}</div>
            </div>
            <div className="ptm mhl border-bottom-hover clickable">
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
