define(function(require) {
  var React = require('react'),
    CSS = require('../css'),
    Collapsible = require('../collapsible'),
    CsrfTokenHiddenInput = require('../csrf_token_hidden_input'),
    Input = require('../form/input'),
    SettingsMenu = require('../settings_menu'),
    BrowserUtils = require('../browser_utils'),
    ifPresent = require('../if_present');

  return React.createClass({
    displayName: 'ApplicationEditor',
    propTypes: {
      apis: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      applicationApiId: React.PropTypes.string,
      recommendedScope: React.PropTypes.string,
      requiredOAuth2ApiConfigId: React.PropTypes.string,
      applicationName: React.PropTypes.string,
      applicationClientId: React.PropTypes.string,
      applicationClientSecret: React.PropTypes.string,
      applicationScope: React.PropTypes.string,
      applicationSaved: React.PropTypes.bool,
      csrfToken: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired,
      callbackUrl: React.PropTypes.string.isRequired,
      mainUrl: React.PropTypes.string.isRequired,
      applicationId: React.PropTypes.string,
      behaviorId: React.PropTypes.string
    },

    getInitialState: function() {
      return {
        applicationApi: this.findApiById(this.props.applicationApiId),
        applicationName: this.props.applicationName || "",
        applicationClientId: this.props.applicationClientId || "",
        applicationClientSecret: this.props.applicationClientSecret || "",
        applicationScope: this.props.applicationScope || this.props.recommendedScope || "",
        hasNamedApplication: this.props.applicationSaved || false,
        shouldRevealApplicationUrl: this.props.applicationSaved || false,
        isSaving: false
      };
    },

    findApiById: function(id) {
      if (id) {
        var matches = this.props.apis.filter(function(api) { return api.apiId === id; });
        if (matches.length > 0) {
          return matches[0];
        }
      }
      return null;
    },

    getCallbackUrl: function() {
      return this.props.callbackUrl;
    },

    getMainUrl: function() {
      return this.props.mainUrl;
    },

    apiIsSet: function() {
      return !!this.state.applicationApi;
    },

    getApplicationApiName: function() {
      return this.state.applicationApi ? this.state.applicationApi.name : "";
    },

    getApplicationApiScopeDocumentationUrl: function() {
      return this.state.applicationApi ? this.state.applicationApi.scopeDocumentationUrl : "";
    },

    getApplicationApiNewApplicationUrl: function() {
      return this.state.applicationApi ? this.state.applicationApi.newApplicationUrl : "";
    },

    getApplicationApiId: function() {
      return this.state.applicationApi ? this.state.applicationApi.apiId : "";
    },

    setApplicationApi: function(api) {
      this.setState({ applicationApi: api }, function() {
        this.refs.applicationName.focus();
        BrowserUtils.replaceQueryParam('apiId', api.apiId);
      });
    },

    reset: function() {
      this.setState({
        applicationApi: null,
        applicationName: "",
        applicationClientId: "",
        applicationScope: "",
        hasNamedApplication: false,
        shouldRevealApplicationUrl: false
      }, function() {
        BrowserUtils.removeQueryParam('apiId');
      });
    },

    getApplicationName: function() {
      return this.state.applicationName;
    },

    applicationNameIsEmpty: function() {
      return !this.getApplicationName();
    },

    setApplicationName: function(name) {
      this.setState({ applicationName: name });
    },

    onApplicationNameEnterKey: function() {
      if (!this.applicationNameIsEmpty()) {
        this.revealApplicationURL();
        this.refs.applicationName.blur();
      }
    },

    revealApplicationURL: function() {
      this.setState({ shouldRevealApplicationUrl: true });
    },

    shouldRevealApplicationUrl: function() {
      return this.state.shouldRevealApplicationUrl;
    },

    getApplicationClientId: function() {
      return this.state.applicationClientId;
    },

    setApplicationClientId: function(value) {
      this.setState({ applicationClientId: value });
    },

    getApplicationClientSecret: function() {
      return this.state.applicationClientSecret;
    },

    setApplicationClientSecret: function(value) {
      this.setState({ applicationClientSecret: value });
    },

    getApplicationScope: function() {
      return this.state.applicationScope;
    },

    setApplicationScope: function(value) {
      this.setState({ applicationScope: value });
    },

    canBeSaved: function() {
      return !!(
        this.getApplicationApiName() && this.getApplicationName() &&
        this.getApplicationClientId() && this.getApplicationClientSecret()
      );
    },

    onSaveClick: function() {
      this.setState({
        isSaving: true
      });
    },

    onFocusExample: function(event) {
      if (event) {
        event.target.select();
      }
    },

    renderBehaviorId: function() {
      var id = this.props.behaviorId;
      if (id && id.length > 0) {
        return (<input type="hidden" name="behaviorId" value={id} />);
      } else {
        return null;
      }
    },

    render: function() {
      return (
        <form action={jsRoutes.controllers.ApplicationController.saveOAuth2Application().url} method="POST">
          <CsrfTokenHiddenInput value={this.props.csrfToken} />
          <input type="hidden" name="apiId" value={this.getApplicationApiId()} />
          <input type="hidden" name="requiredOAuth2ApiConfigId" value={this.props.requiredOAuth2ApiConfigId} />
          <input type="hidden" name="id" value={this.props.applicationId} />
          <input type="hidden" name="teamId" value={this.props.teamId} />
          {this.renderBehaviorId()}

          <div className="bg-light">
            <div className="container pbm">
              {this.renderHeader()}
            </div>
          </div>

          <div className="flex-container">
            <div className="container flex flex-center">
              <div className="columns">
                <div className="column column-one-quarter">
                  <SettingsMenu activePage="oauthApplications" />
                </div>
                <div className="column column-three-quarters bg-white border-radius-bottom-left ptxl pbxxxxl phxxxxl">
                  <Collapsible revealWhen={!this.apiIsSet()}>
                    {this.renderChooseApi()}
                  </Collapsible>
                  <Collapsible revealWhen={this.apiIsSet()}>
                    {this.renderConfigureApplication()}
                  </Collapsible>
                </div>
              </div>
            </div>
            <div className="flex flex-left"></div>
            <div className="flex flex-right bg-white"></div>
          </div>

          <footer className={
            "position-fixed-bottom position-z-front border-top ptm " +
            (this.canBeSaved() ? "bg-white" : "bg-light-translucent" +
            CSS.visibleWhen(this.shouldRevealApplicationUrl()))
          }>
            <div className="container">
              <div className="columns mobile-columns-float">
                <div className="column column-one-quarter"></div>
                <div className="column column-three-quarters plxxxxl prm">
                  <button type="submit"
                    className={"button-primary mrs mbm " + (this.state.isSaving ? "button-activated" : "")}
                    disabled={!this.canBeSaved()}
                    onClick={this.onSaveClick}
                  >
                    <span className="button-labels">
                      <span className="button-normal-label">
                        <span className="mobile-display-none">Save changes</span>
                        <span className="mobile-display-only">Save</span>
                      </span>
                      <span className="button-activated-label">Saving…</span>
                    </span>
                  </button>
                </div>
              </div>
            </div>
          </footer>
        </form>
      );
    },

    renderHeader: function() {
      return (
        <h3 className="mvn ptxxl type-weak display-ellipsis">
          <span className="mrs">
            <a href={jsRoutes.controllers.ApplicationController.listOAuth2Applications().url}>API applications</a>
          </span>
          <span className="mhs">→</span>
          {this.renderApplicationHeader()}
        </h3>
      );
    },

    renderApplicationHeader: function() {
      if (!this.apiIsSet()) {
        return (
          <span className="mhs">Add an application</span>
        );
      } else if (!this.props.applicationSaved) {
        return (
          <span>
            <span className="mhs">
              <button className="button-raw" onClick={this.reset}>Add an application</button>
            </span>
            <span className="mhs">→</span>
            <span className="mhs">{this.getApplicationApiName()}</span>
          </span>
        );
      } else {
        return (
          <span>
            <span className="mhs">Edit an application</span>
            <span className="mhs">→</span>
            <span className="mhs">{this.getApplicationName() || (<span className="type-disabled">Untitled</span>)}</span>
          </span>
        );
      }
    },

    renderChooseApi: function() {
      return (
        <div>
          <p className="mtm">
            <span>Choose an API you would like to integrate with Ellipsis. This will allow your behaviors to read </span>
            <span>and/or write data from that product. You can create multiple applications for a single API, each </span>
            <span>with a different level of access.</span>
          </p>

          <div className="mvxl">
            {this.props.apis.map(function(api, index) {
              return (
                <button type="button" key={"apiTypeButton" + index} className="button-l mrl mbl" onClick={this.setApplicationApi.bind(this, api)}>
                  {ifPresent(api.imageUrl, url => (
                    <img src={url} width="24" height="24" className="mrm align-m mbxs" />
                  ))}
                  <span className="type-black">{api.name}</span>
                </button>
              );
            }, this)}
          </div>
        </div>
      );
    },

    renderConfigureApplication: function() {
      return (
        <div>
          <p className="mtm mbxl">Configure a new {this.getApplicationApiName()} application so your behaviors can access data from a {this.getApplicationApiName()} account.</p>

          <div>
            <h4 className="mbn position-relative">
              <span className="position-hanging-indent">1</span>
              <span> Enter a name for this application</span>
            </h4>
            <p className="type-s">The name should help differentiate this from any other {this.getApplicationApiName()} applications you may have with different kinds of access, or access to a different set of data.</p>

            <div className="mbxxl columns">
              <div className="column column-two-thirds">
                <Input
                  ref="applicationName"
                  name="name"
                  value={this.getApplicationName()}
                  placeholder={"e.g. " + this.getApplicationApiName() + " read-only"}
                  className="form-input-borderless form-input-large"
                  onChange={this.setApplicationName}
                  onEnterKey={this.onApplicationNameEnterKey}
                />
              </div>
            </div>


            <Collapsible revealWhen={!this.shouldRevealApplicationUrl()}>
              <div className="mvxl">
                <button type="button"
                  className="button-primary"
                  disabled={this.applicationNameIsEmpty()}
                  onClick={this.revealApplicationURL}>
                  Continue
                </button>
              </div>
            </Collapsible>

            <Collapsible revealWhen={this.shouldRevealApplicationUrl()}>
              <hr className="mvxxxl" />

              <div className="mvm">
                <h4 className="mbn position-relative">
                  <span className="position-hanging-indent">2</span>
                  <span>Register a new OAuth developer application on your {this.getApplicationApiName()} account. </span>
                  {ifPresent(this.getApplicationApiNewApplicationUrl(), url => (
                    <a href={url} target="_blank">Go to {this.getApplicationApiName()} ↗︎</a>
                  ))}
                </h4>
                <ul className="type-s list-space-l mvl">
                  <li>You can set the name and description to whatever you like.</li>
                  <li>
                    <div>Copy and paste this for the <b>callback URL</b> (sometimes called <b>redirect URL</b>):</div>
                    <input type="text" readOnly={true} className="box-code-example display-ellipsis mtl" value={this.getCallbackUrl()} onFocus={this.onFocusExample} />
                  </li>
                  <li>
                    <div>If there is a homepage, application or other URL option, you can set it to:</div>
                    <input type="text" readOnly={true} className="box-code-example display-ellipsis mtl" value={this.getMainUrl()} onFocus={this.onFocusExample} />
                  </li>
                </ul>
              </div>

              <hr className="mvxxxl" />

              <div className="mvm">
                <h4 className="mbn position-relative">
                  <span className="position-hanging-indent">3</span>
                  <span>Paste the client ID and client secret from your {this.getApplicationApiName()} OAuth application</span>
                </h4>
                <p className="type-s">
                  These values will be generated by {this.getApplicationApiName()} after you’ve saved an application there in step 2.
                </p>

                <div className="columns mtl">
                  <div className="column column-one-half">
                    <h5>Client ID</h5>
                    <Input className="form-input-borderless type-monospace"
                      placeholder="Enter identifier"
                      name="clientId"
                      value={this.getApplicationClientId()}
                      onChange={this.setApplicationClientId}
                      disableAuto={true}
                    />
                  </div>
                  <div className="column column-one-half">
                    <h5>Client secret</h5>
                    <Input className="form-input-borderless type-monospace"
                      placeholder="Enter secret"
                      name="clientSecret"
                      value={this.getApplicationClientSecret()}
                      onChange={this.setApplicationClientSecret}
                      disableAuto={true}
                    />
                  </div>
                </div>
              </div>

              <hr className="mvxxxl" />

              <div className="mvm">
                <h4 className="mbn position-relative">
                  <span className="position-hanging-indent">4</span>
                  <span>Set the scope to specify the kind of access to {this.getApplicationApiName()} data you want.</span>
                </h4>
                {ifPresent(this.getApplicationApiScopeDocumentationUrl(), url => (
                  <p className="type-s">
                    <span>Use the <a href={url} target="_blank">scope documentation at {this.getApplicationApiName()}</a> to determine </span>
                    <span>the correct value for your application.</span>
                  </p>
                ))}

                <div className="columns">
                  <div className="column column-one-third">
                    <Input className="form-input-borderless type-monospace"
                      name="scope"
                      value={this.getApplicationScope()}
                      onChange={this.setApplicationScope}
                      placeholder="Enter scope value"
                      disableAuto={true}
                    />
                  </div>
                </div>
              </div>
            </Collapsible>
          </div>
        </div>
      );
    }
  });
});
