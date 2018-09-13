import * as React from 'react';
import Collapsible from '../shared_ui/collapsible';
import * as Constants from '../lib/constants';
import HelpPanel from '../help/panel';
import autobind from "../lib/autobind";
import {RequiredOAuthApplication} from "../models/oauth";

interface Props {
  envVariableNames: Array<string>,
  oauthApiAccessTokens: Array<RequiredOAuthApplication>,
  onAddNewEnvVariable: () => void,
  onCollapseClick: () => void,
  isDataType: boolean
}

interface State {
  expandedItems: Array<string>
}

class BehaviorCodeHelp extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = {
      expandedItems: []
    };
  }

  getExpandedItems(): Array<string> {
    return this.state.expandedItems;
  }
  hasExpandedItem(itemName: string): boolean {
    return this.getExpandedItems().includes(itemName);
  }
  toggleExpandedItem(itemName: string): void {
    const hasExpanded = this.hasExpandedItem(itemName);
    const newItems = hasExpanded ?
      this.getExpandedItems().filter((ea) => ea !== itemName) :
      this.getExpandedItems().concat(itemName);
    this.setState({ expandedItems: newItems });
  }
  toggleSuccessExamples(): void {
    this.toggleExpandedItem('success');
  }
  toggleErrorExamples(): void {
    this.toggleExpandedItem('error');
  }
  toggleEnvVariables(): void {
    this.toggleExpandedItem('envVariables');
  }
  toggleApiAccessTokens(): void {
    this.toggleExpandedItem('apiAccessTokens');
  }
  toggleUserInfo(): void {
    this.toggleExpandedItem('userInfo');
  }
  toggleEllipsisObject(): void {
    this.toggleExpandedItem('ellipsisObject');
  }
  toggleDataTypeSuccessExamples(): void {
    this.toggleExpandedItem('dataTypeSuccess');
  }
  renderExpandLabelFor(labelText: string, itemName: string) {
    return (
      <span>
        <span className="display-inline-block" style={{ width: '0.8em' }}>
          {this.hasExpandedItem(itemName) ? "▾" : "▸"}
        </span>
        <span> {labelText}</span>
      </span>
    );
  }
  renderEnvVarList() {
    var vars = this.props.envVariableNames;
    if (vars.length > 0) {
      return vars.map((name, index) => (
        <div key={`envVar${index}`}>
          <code>{name}:</code>
          <span className="bg-dark-translucent plxxxl mls"
            title="For security, environment variable values are not displayed."/>
        </div>
      ));
    } else {
      return (
        <p className="man">There are no environment variables configured.</p>
      );
    }
  }

  getAllApiAccessTokens(): Array<RequiredOAuthApplication> {
    return this.props.oauthApiAccessTokens || [];
  }

  renderApiAccessTokenList() {
    var tokenList = this.getAllApiAccessTokens();
    if (tokenList.length > 0) {
      return tokenList.map((token, index) => (
        <div key={`accessToken${index}`} className="mbs">
          <code className="box-code-example">{token.nameInCode}</code> — {token.configName()}
        </div>
      ));
    } else {
      return (
        <p className="man">This skill has no API access tokens available.</p>
      );
    }
  }
  renderForAction() {
    return (
      <div>
        <p>
          <span>The function will automatically receive the <code className="type-bold">ellipsis</code> </span>
          <span>object, which contains important methods and properties, along with any inputs </span>
          <span>you’ve defined in the action.</span>
        </p>

        <p>
          <span>Ensure your function calls the <code className="type-bold">success</code> or </span>
          <span><code className="type-bold">noResponse</code> response method to finish, for example:</span>
        </p>

        <div className="box-code-example mvl">ellipsis.success("It worked!");</div>

        <div className="box-code-example mvl">ellipsis.noResponse();</div>
      </div>
    );
  }
  renderCallbacksForActions() {
    return (
      <div className="column-group">
        <div className="column-row">
          <div className="column column-shrink pbl prxl"><pre>  success(successResult)</pre></div>
          <div className="column column-expand pbl">
            <div>
              <span>Ends the function, passing <span className="type-monospace type-bold">successResult</span> to </span>
              <span>the response template which is then displayed to the user. </span>
              <span><span className="type-monospace type-bold">successResult</span> can be a string, array, </span>
              <span>or object. </span>
              <button type="button" className="button-raw" onClick={this.toggleSuccessExamples}>
                {this.renderExpandLabelFor('Examples', 'success')}
              </button>
            </div>
            <Collapsible revealWhen={this.hasExpandedItem('success')}>
              <div className="box-code-example mvs">
                {'ellipsis.success("The answer is: " + answer);'}
              </div>
              <div className="box-code-example mvs">
                {"ellipsis.success(\u007B firstName: 'Abraham', lastName: 'Lincoln' \u007D);"}
              </div>
              <div className="box-code-example mvs">
                {"ellipsis.success(['Mercury', 'Venus', 'Earth', 'Mars', 'Jupiter', 'Saturn', 'Neptune', 'Uranus']);"}
              </div>
            </Collapsible>
          </div>
        </div>

        <div className="column-row">
          <div className="column column-shrink pbl prxl"><pre>  noResponse()</pre></div>
          <div className="column column-expand pbl">
            <span>Ends the function without sending a response.</span>
          </div>
        </div>
      </div>
    );
  }

  renderForDataType() {
    return (
      <div>
        <ul>
          <li>
            <span>The function will automatically receive the <code className="type-bold">ellipsis</code> </span>
            <span>object, which contains important methods and properties.</span>
          </li>
          <li>
            <span>If your data type requests input, your function will also receive a </span>
            <span><code className="type-bold">searchQuery</code> parameter.</span>
          </li>
        </ul>

        <p>
          <span>Your function should call <code className="type-bold">ellipsis.success</code> with an array </span>
          <span>of items to be shown to the user as a list of choices.</span>
        </p>

        <p>
          <span>Each item must have <code className="type-bold">id</code> and </span>
          <span><code className="type-bold">label</code> properties. <code>id</code> should be </span>
          <span>unique and the <code>label</code> will be shown to the user.</span>
        </p>

        <p>
          <button type="button" className="button-raw" onClick={this.toggleDataTypeSuccessExamples}>
            {this.renderExpandLabelFor("Show examples", "dataTypeSuccess")}
          </button>
        </p>

        <Collapsible revealWhen={this.hasExpandedItem("dataTypeSuccess")}>
          <p>
            The user will be asked to choose from 3 items:
          </p>

          <pre className="box-code-example mvl">{
  `ellipsis.success([
    { id: "A", label: "The letter A" },
    { id: "B", label: "The letter B" },
    { id: "C", label: "The letter C" }
  ]);`
          }</pre>

          <p>
            If you ask for user input first, you can return a single item if there's only one possible result:
          </p>

          <pre className="box-code-example mvl">{
  `ellipsis.success([
    { id: "A", label: "The letter A" }
  ]);`
          }</pre>

          <p>
            <span>If you ask for user input, and it was invalid or there are no matches, return an empty array. </span>
            <span>The user will be asked to provide new input and then the function will run again.</span>
          </p>

          <pre className="box-code-example mvl">{
  `ellipsis.success([]);`
          }</pre>
        </Collapsible>

      </div>
    );
  }

  renderCallbacksForDataTypes() {
    return (
      <div className="column-group">
        <div className="column-row">
          <div className="column column-shrink pbl prxl"><pre>  success(arrayOfItems)</pre></div>
          <div className="column column-expand pbl">
            <div>
              <span>Ends the function with <span className="type-monospace type-bold">arrayOfItems</span> which </span>
              <span>Ellipsis will present to the user as a list of choices. </span>
            </div>
          </div>
        </div>

      </div>
    );
  }

  render() {
    return (
      <HelpPanel
        heading={this.props.isDataType ? "Data type functions" : "Action functions"}
        onCollapseClick={this.props.onCollapseClick}
      >
        <p>
          <span>Write a JavaScript function compatible with </span>
          <span><a href={Constants.NODE_JS_DOCS_URL} target="_blank">Node.js {Constants.NODE_JS_VERSION}</a>. </span>
        </p>

        {this.props.isDataType ? this.renderForDataType() : this.renderForAction()}

        <p>
          <span>To indicate an error, use the <code className="type-bold">ellipsis.Error</code> class, for example:</span>
        </p>

        <div className="box-code-example mvl">{'throw new ellipsis.Error("error 404");'}</div>

        <div className="box-code-example mvl">{'throw new ellipsis.Error("error 404", { userMessage: "Something went wrong!" });'}</div>

        <p>
          <span>You can <code>require()</code> any <a href="https://www.npmjs.com/">NPM package</a> or any library you add to your skill.</span>
        </p>

        <pre className="box-code-example mvl">{'// Use the request npm module\nconst request = require("request");'}</pre>

        <h5>The ellipsis object</h5>
        <p>
          <span>The function will receive a parameter called <code>ellipsis</code>, an object with useful run-time properties. </span>
          <button type="button" className="button-raw" onClick={this.toggleEllipsisObject}>
            {this.renderExpandLabelFor("Show properties", "ellipsisObject")}
          </button>
        </p>

        <Collapsible revealWhen={this.hasExpandedItem("ellipsisObject")}>

          <div className="columns columns-elastic">
            <div className="column-group">
              <div className="column-row">
                <div className="column column-shrink pbl prxl"><pre>{"ellipsis \u007B"}</pre></div>
                <div className="column column-expand pbl" />
              </div>
            </div>

            {this.props.isDataType ? this.renderCallbacksForDataTypes() : this.renderCallbacksForActions()}

            <div className="column-group">
              <div className="column-row">
                <div className="column column-shrink pbl prxl"><pre>  Error</pre></div>
                <div className="column column-expand pbl">
                  <div>
                    <span>An error class constructor you can use with the <code>throw</code> statement.</span>
                    <span>This will interrupt your function and show an error message to the user. </span>
                    <button type="button" className="button-raw" onClick={this.toggleErrorExamples}>
                      {this.renderExpandLabelFor('Example', 'error')}
                    </button>
                  </div>
                  <Collapsible revealWhen={this.hasExpandedItem('error')}>
                    <div className="box-code-example mvs">
                      {'throw new ellipsis.Error("API error");'}
                    </div>
                    <div className="box-code-example mvs">
                      {'throw new ellipsis.Error("API error", { userMessage: "Something went wrong." });'}
                    </div>
                  </Collapsible>
                </div>
              </div>

              <div className="column-row">
                <div className="column column-shrink pbl prxl"><pre>{`  env {}`}</pre></div>
                <div className="column column-expand pbl">
                  <div>
                    <span>Contains any configured <b>environment variables</b> as properties, accessible by name. </span>
                    <button type="button" className="button-raw" onClick={this.toggleEnvVariables}>
                      {this.renderExpandLabelFor('Show list', 'envVariables')}
                    </button>
                  </div>
                  <Collapsible revealWhen={this.hasExpandedItem('envVariables')}>
                    <div className="phs pvxs bg-blue-lightest type-weak border border-blue position-relative mbs">
                      {this.renderEnvVarList()}
                    </div>
                    <button type="button" className="button-s" onClick={this.props.onAddNewEnvVariable}>
                      Add new environment variable
                    </button>
                  </Collapsible>
                </div>
              </div>

              <div className="column-row">
                <div className="column column-shrink pbl prxl"><pre>{'  accessTokens {}'}</pre></div>
                <div className="column column-expand pbl">
                  <div>
                    <span>Contains any <b>third-party API access tokens</b> available to the skill. </span>
                    <button type="button" className="button-raw" onClick={this.toggleApiAccessTokens}>
                      {this.renderExpandLabelFor('Show list', 'apiAccessTokens')}
                    </button>
                  </div>
                  <Collapsible revealWhen={this.hasExpandedItem('apiAccessTokens')}>
                    {this.renderApiAccessTokenList()}
                  </Collapsible>
                </div>
              </div>

              <div className="column-row">
                <div className="column column-shrink pbl prxl">
                  <pre>{
                    `  event {
    originalEventType
  }`
                  }</pre>
                </div>
                <div className="column column-expand pbl">
                  <div>
                    <span>The <code>originalEventType</code> reveals what originally triggered this action, as a string. </span>
                    <span>Event types include: <code className="type-bold">"scheduled"</code>, <code className="type-bold">"api"</code>, </span>
                    <span><code className="type-bold">"test"</code>, <code className="type-bold">"chat"</code>, and <code className="type-bold">"web"</code>.</span>
                  </div>
                </div>
              </div>

              <div className="column-row">
                <div className="column column-shrink pbl prxl">
                  <pre>{
`  teamInfo {
    timeZone
  }`
                }</pre>
                </div>
                <div className="column column-expand pbl">
                  <div>
                    <span>The teamInfo object contains the time zone ID set for the team, e.g. <code>"America/New_York"</code></span>
                  </div>
                </div>
              </div>

              <div className="column-row">
                <div className="column column-shrink pbl prxl">
                  <pre className="type-black">{'  userInfo {}'}</pre>
                </div>
                <div className="column column-expand pbl">
                  <div>
                    <span>The userInfo object contains optional data specific to who runs an action as well as the message they wrote. </span>
                    <button className="button-raw" type="button" onClick={this.toggleUserInfo}>
                      {this.renderExpandLabelFor('Show list', 'userInfo')}
                    </button>
                  </div>

                  <div>
                    <Collapsible revealWhen={this.hasExpandedItem('userInfo')}>
                      <h5>User info properties directly on the userInfo object.</h5>
                      <pre className="box-code-example">{
`userInfo {
  context: string // e.g. "slack"
  userName: string
  ellipsisUserId: string
  userIdForContext: string
  fullName: string
  email: string
  timeZone: string
}`}</pre>

                      <h5>Message properties:</h5>
                      <div><pre className="box-code-example">{
`userInfo {
  messageInfo {
    medium: string // e.g. "slack" or "test"
    channel: string // channel ID
    usersMentioned: object[] // array of userInfo objects (same as above)
    permalink: string
    details: {
      channelMembers: string[] // array of user IDs
      channelName: string
      name: string
      profile: {
        firstName: string
        lastName: string
        realName: string
      }
    }
  }
}`}</pre></div>
                    </Collapsible>
                  </div>
                </div>
              </div>
            </div>
          </div>

        </Collapsible>

      </HelpPanel>
    );
  }
}

export default BehaviorCodeHelp;
