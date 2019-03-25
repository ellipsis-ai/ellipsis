import * as React from 'react';
import APIRequestHelp from './api_request_help';
import Collapsible from '../../shared_ui/collapsible';
import CSRFTokenHiddenInput from '../../shared_ui/csrf_token_hidden_input';
import Formatter, {Timestamp} from '../../lib/formatter';
import HelpButton from '../../help/help_button';
import FormInput from '../../form/input';
import {PageRequiredProps} from '../../shared_ui/page';
import SettingsPage from '../../shared_ui/settings_page';
import autobind from "../../lib/autobind";

const revokeForm = jsRoutes.controllers.APITokenController.revokeToken();
const createForm = jsRoutes.controllers.APITokenController.createToken();

interface ApiToken {
  id: string
  label: string
  lastUsed?: Option<string>
  createdAt: Timestamp
  isRevoked: boolean
}

export interface ApiTokenGeneratorProps {
  csrfToken: string
  isAdmin: boolean
  teamId: string
  tokens: Array<ApiToken>
  justCreatedTokenId?: Option<string>
  canGenerateTokens: boolean
}

interface State {
  newTokenLabel: string
}

type Props = ApiTokenGeneratorProps & PageRequiredProps

class ApiTokenGenerator extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      autobind(this);
      this.state = {
        newTokenLabel: ""
      }
    }

    sortByMostRecent(tokens: Array<ApiToken>): Array<ApiToken> {
      return tokens.slice().sort((a, b) => {
        const first = Number(new Date(a.createdAt));
        const second = Number(new Date(b.createdAt));
        if (first > second) {
          return -1;
        } else if (first < second) {
          return 1;
        } else {
          return 0;
        }
      });
    }

    getTokens(): Array<ApiToken> {
      return this.sortByMostRecent(this.props.tokens);
    }

    getNewTokenLabel(): string {
      return this.state.newTokenLabel;
    }

    setNewTokenLabel(value: string): void {
      this.setState({ newTokenLabel: value });
    }

    shouldHighlightToken(token: ApiToken): boolean {
      return this.props.justCreatedTokenId === token.id;
    }

    toggleApiHelp(): void {
      this.props.onToggleActivePanel('ellipsisApiHelp');
    }

    render() {
      return (
        <SettingsPage teamId={this.props.teamId} activePage={"apiTokens"} isAdmin={this.props.isAdmin}>
          <p>
            <span>Generate API tokens to make requests to Ellipsis that trigger a response. </span>
            <span>For better security, use each token only for a single purpose.</span>
          </p>

          <p>
            <HelpButton className="mrs" onClick={this.toggleApiHelp} toggled={this.props.activePanelName === 'ellipsisApiHelp'} />
            <button type="button" className="button-raw" onClick={this.toggleApiHelp}>How to make requests</button>
          </p>

          {this.getTokens().length > 0 ? this.renderTokenList() : this.renderNoTokens()}

          {this.renderTokenCreator()}

          {this.props.onRenderFooter((
            <Collapsible revealWhen={this.props.activePanelName === 'ellipsisApiHelp'}>
              <APIRequestHelp onCollapse={this.toggleApiHelp} />
            </Collapsible>
          ))}
        </SettingsPage>
      );
    }

    renderTokenList() {
      return (
        <div className="columns columns-elastic mobile-columns-float">
          <div className="column-group">
            <div className="column-row">
              <div className="column column-expand"><h6 className="mvs">Label</h6></div>
              <div className="column column-shrink"><h6 className="mvs">Token</h6></div>
              <div className="column column-shrink"><h6 className="mvs">Created</h6></div>
              <div className="column column-shrink display-ellipsis"><h6 className="mvs">Last used</h6></div>
              <div className="column column-shrink"><h6 className="mvs" /></div>
            </div>
            {this.getTokens().map((token, index) => {
              return (
                <div className="column-row" key={`token${index}`}>
                  <div className="column column-expand border-top ptm pbs">
                    <span
                      className={this.shouldHighlightToken(token) ? "type-bold" : ""}
                    >{token.label}</span>
                  </div>
                  <div className="column column-shrink display-ellipsis border-top pvs">
                    <div
                      className={
                        "box-code-example " +
                        (this.shouldHighlightToken(token) ? " box-code-example-highlighted " : "")
                      }>
                      {token.id}
                    </div>
                  </div>
                  <div className="column column-shrink type-s type-weak type-italic display-ellipsis border-top pvs">
                    {Formatter.formatTimestampRelativeIfRecent(token.createdAt)}
                  </div>
                  <div className="column column-shrink type-s type-weak display-ellipsis border-top pvs">
                    {token.lastUsed ? Formatter.formatTimestampRelativeIfRecent(token.lastUsed) : "Never"}
                  </div>
                  <div className="column column-shrink border-top pvs">
                    {token.isRevoked ? (<i>Token revoked</i>) : (
                      <form action={revokeForm.url} method={revokeForm.method}>
                        <CSRFTokenHiddenInput value={this.props.csrfToken} />
                        <input type="hidden" name="id" value={token.id} />
                        <button type="submit" className="button-s button-shrink">Revoke</button>
                      </form>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      );
    }

    renderNoTokens() {
      return (
        <div>
          <hr />
          <p><i>There are no active tokens.</i></p>
        </div>
      );
    }

    renderTokenCreator() {
      if (this.props.canGenerateTokens) {
        return (
          <div className="mvxl">
            <form action={createForm.url} method={createForm.method}>
              <h4 className="mbn">Generate a new token</h4>
              <p className="type-s">Enter a label to describe what this token will be used for.</p>

              <CSRFTokenHiddenInput value={this.props.csrfToken}/>
              <input type="hidden" name="teamId" value={this.props.teamId}/>

              <div className="columns mbm">
                <div className="column column-one-third">
                  <FormInput name="label" value={this.getNewTokenLabel()} onChange={this.setNewTokenLabel}/>
                </div>
                <div className="column column-one-third">
                  <button type="submit" className="button-primary" disabled={!this.getNewTokenLabel()}>Generate</button>
                </div>
              </div>
            </form>
          </div>
        );
      } else {
        return (
          <div className="mvxl">
            <p className="type-pink type-bold type-italic">
              You do not have access to generate tokens for this team.
            </p>
          </div>
        );
      }
    }
}

export default ApiTokenGenerator;
