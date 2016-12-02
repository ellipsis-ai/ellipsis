define(function(require) {
  var React = require('react'),
      Collapsible = require('../collapsible'),
      CSRFTokenHiddenInput = require('../csrf_token_hidden_input'),
      Formatter = require('../formatter'),
      HelpButton = require('../help/help_button'),
      HelpPanel = require('../help/panel'),
      Input = require('../form/input'),
      SettingsMenu = require('../settings_menu');

  var revokeForm = jsRoutes.controllers.APITokenController.revokeToken();
  var createForm = jsRoutes.controllers.APITokenController.createToken();

  return React.createClass({
    displayName: "ApiTokenGenerator",
    propTypes: {
      csrfToken: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired,
      tokens: React.PropTypes.arrayOf(React.PropTypes.shape({
        id: React.PropTypes.string.isRequired,
        label: React.PropTypes.string.isRequired,
        lastUsed: React.PropTypes.number,
        createdAt: React.PropTypes.number.isRequired,
        isRevoked: React.PropTypes.bool.isRequired
      })).isRequired,
      justCreatedTokenId: React.PropTypes.string
    },

    sortByMostRecent: function(tokens) {
      return tokens.map(t => t).sort((a, b) => a.createdAt < b.createdAt);
    },

    getInitialState: function() {
      return {
        activePanel: null,
        newTokenLabel: "",
        tokens: this.sortByMostRecent(this.props.tokens)
      };
    },

    getTokens: function() {
      return this.state.tokens;
    },

    getNewTokenLabel: function() {
      return this.state.newTokenLabel;
    },

    setNewTokenLabel: function(value) {
      this.setState({ newTokenLabel: value });
    },

    shouldHighlightToken: function(token) {
      return this.props.justCreatedTokenId === token.id;
    },

    getActivePanel: function() {
      return this.state.activePanel;
    },

    toggleApiHelp: function() {
      this.setState({ activePanel: this.getActivePanel() === "ellipsisApiHelp" ? null : "ellipsisApiHelp" });
    },

    render: function() {
      return (
        <div className="flex-rows">
          <div className="bg-light">
            <div className="container container-wide pbm">
              <h3 className="mvn ptxxl type-weak display-ellipsis">Ellipsis API tokens</h3>
            </div>
          </div>
          <div className="flex-columns">
            <div className="flex-column flex-column-left container container-wide prn">
              <div className="columns">
                <div className="column column-one-quarter">
                  <SettingsMenu activePage="apiTokens"/>
                </div>
                <div className="column column-three-quarters bg-white border-radius-bottom-left ptxxl pbxxxxl phxxxxl">
                  <p>
                    <span>Generate API tokens to make requests to Ellipsis that trigger a response. </span>
                    <span>For better security, use each token only for a single purpose.</span>
                  </p>

                  <p>
                    <HelpButton className="mrs" onClick={this.toggleApiHelp} toggled={this.getActivePanel() === 'ellipsisApiHelp'} />
                    <button type="button" className="button-raw" onClick={this.toggleApiHelp}>How to make requests</button>
                  </p>

                  {this.getTokens().length > 0 ? this.renderTokenList() : this.renderNoTokens()}

                  {this.renderTokenCreator()}

                </div>
              </div>
            </div>
            <div className="flex-column flex-column-right bg-white"></div>
          </div>

          <footer ref="footer" className="position-fixed-bottom position-z-front border-top">
            <Collapsible ref="ellipsisApiHelp" revealWhen={this.getActivePanel() === 'ellipsisApiHelp'}>
              <HelpPanel
                heading="Sending API requests to Ellipsis"
                onCollapseClick={this.toggleApiHelp}
              >
                <p>
                  Any application can send a message to Ellipsis by making a GET request to <code className="box-code-example">https://bot.ellipsis.ai/post_message</code>.
                </p>

                <p>
                  The request should include the following parameters:
                </p>

                <ul>
                  <li><code className="box-code-example">message:</code> a message which would normally trigger a skill</li>
                  <li><code className="box-code-example">responseContext:</code> use <code className="box-code-example">slack</code> to see a response in Slack</li>
                  <li><code className="box-code-example">channel:</code> the name of the channel to send a response</li>
                  <li><code className="box-code-example">token:</code> a valid API token</li>
                </ul>
              </HelpPanel>
            </Collapsible>
          </footer>
        </div>
      );
    },

    renderTokenList: function() {
      return (
        <div className="columns columns-elastic mobile-columns-float">
          <div className="column-group">
            <div className="column-row type-bold">
              <div className="column column-expand type-label pvs">Label</div>
              <div className="column column-shrink type-label pvs">Token</div>
              <div className="column column-shrink type-label pvs">Created</div>
              <div className="column column-shrink type-label pvs display-ellipsis">Last used</div>
              <div className="column column-shrink type-label pvs"></div>
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
    },

    renderNoTokens: function() {
      return (
        <div>
          <hr />
          <p><i>There are no active tokens.</i></p>
        </div>
      );
    },

    renderTokenCreator: function() {
      return (
        <div className="mvxl">
          <form action={createForm.url} method={createForm.method}>
            <h4 className="mbn">Generate a new token</h4>
            <p className="type-s">Enter a label to describe what this token will be used for.</p>

            <CSRFTokenHiddenInput value={this.props.csrfToken} />
            <input type="hidden" name="teamId" value={this.props.teamId} />

            <div className="columns mbm">
              <div className="column column-one-third">
                <Input name="label" value={this.getNewTokenLabel()} onChange={this.setNewTokenLabel} />
              </div>
              <div className="column column-one-third">
                <button type="submit" className="button-primary" disabled={!this.getNewTokenLabel()}>Generate</button>
              </div>
            </div>
          </form>
        </div>
      );
    }
  });
});
