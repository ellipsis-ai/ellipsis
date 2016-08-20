define(function(require) {
  var React = require('react'),
    Collapsible = require('../collapsible'),
    CsrfTokenHiddenInput = require('../csrf_token_hidden_input'),
    Input = require('../form/input');

  return React.createClass({
    displayName: 'ApplicationEditor',
    propTypes: {
      apis: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      applicationApi: React.PropTypes.string,
      applicationName: React.PropTypes.string,
      applicationClientId: React.PropTypes.string,
      applicationClientSecret: React.PropTypes.string,
      applicationScope: React.PropTypes.string,
      csrfToken: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired
    },

    getInitialState: function() {
      return {
        applicationApi: this.props.applicationApi || null,
        applicationName: this.props.applicationName || "",
        applicationClientId: this.props.applicationClientId || "",
        applicationClientSecret: this.props.applicationClientSecret || "",
        applicationScope: this.props.applicationScope || "",
        hasNamedApplication: false,
        shouldRevealApplicationUrl: false,
        isSaving: false
      };
    },

    apiIsSet: function() {
      return !!this.state.applicationApi;
    },

    getApplicationApiName: function() {
      return this.state.applicationApi ? this.state.applicationApi.name : "";
    },

    getApplicationApiId: function() {
      return this.state.applicationApi ? this.state.applicationApi.apiId : "";
    },

    setApplicationApi: function(api) {
      this.setState({ applicationApi: api }, function() {
        this.refs.applicationName.focus();
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

    render: function() {
      return (
        <form action={jsRoutes.controllers.ApplicationController.saveOAuth2Application} method="POST">
          <CsrfTokenHiddenInput
            value={this.props.csrfToken}
          />
          <input type="hidden" name="apiId" value={this.getApplicationApiId()} />
          <div className="bg-light">
            <div className="container pbm">
              {this.renderHeader()}
            </div>
          </div>

          <div className="container">
            <div className="columns">
              <div className="column column-one-quarter">
              </div>
              <div className="column column-three-quarters bg-white ptxl pbxxxxl phxxxxl">
                <Collapsible revealWhen={!this.apiIsSet()}>
                  {this.renderChooseApi()}
                </Collapsible>
                <Collapsible revealWhen={this.apiIsSet()}>
                  {this.renderConfigureApplication()}
                </Collapsible>
              </div>
            </div>
          </div>

          <footer className="position-fixed-bottom position-z-front">
            <div className="container">
              <div className="columns mobile-columns-float">
                <div className="column column-one-quarter"></div>
                <div className={"column column-three-quarters border-top ptm plxxxxl prm " +
                  (this.canBeSaved() ? "bg-white" : "bg-light-translucent")
                }>
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
          <span className="mrs">API applications</span>
          <span className="mhs">→</span>
          {this.renderApplicationHeader()}
        </h3>
      );
    },

    renderApplicationHeader: function() {
      if (this.apiIsSet()) {
        return (
          <span>
            <span className="mhs"><button className="button-raw" onClick={this.reset}>Add an application</button></span>
            <span className="mhs">→</span>
            <span className="mhs">{this.getApplicationApiName()}</span>
          </span>
        );
      } else {
        return (
          <span className="mhs">Add an application</span>
        );
      }
    },

    renderChooseApi: function() {
      return (
        <div>
          <p>
            <span>Choose an API you would like to integrate with Ellipsis. This will allow your behaviors to read </span>
            <span>and/or write data from that product. You can create multiple applications for a single API, each </span>
            <span>with a different level of access.</span>
          </p>

          <div className="mvxl">
            {this.props.apis.map(function(api, index) {
              return (
                <button type="button" key={"apiTypeButton" + index} className="mrl mbl" onClick={this.setApplicationApi.bind(this, api)}>{api.name}</button>
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
              <span className="position-hanging-indent">1.</span>
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
                  <span className="position-hanging-indent">2.</span>
                  <span>Register a new OAuth developer application on your GitHub account. </span>
                  <a href="https://github.com/settings/applications/new" target="_blank">Go to GitHub ↗︎</a>
                </h4>
                <ul className="type-s list-space-l mvl">
                  <li>You can set the name, homepage and description to whatever you like.</li>
                  <li>
                    <div>Copy and paste this for the authorization callback URL:</div>
                    <div className="box-code-example mtl">http://something.or.other/</div>
                  </li>
                </ul>
              </div>

              <hr className="mvxxxl" />

              <div className="mvm">
                <h4 className="mbn position-relative">
                  <span className="position-hanging-indent">3.</span>
                  <span>Paste the client ID and client secret from your {this.getApplicationApiName()} OAuth application</span>
                </h4>
                <p className="type-s">
                  These values will be generated by {this.getApplicationApiName()} after you’ve saved an application there in step 2.
                </p>

                <div className="columns mtl">
                  <div className="column column-one-third">
                    <h5>Client ID</h5>
                    <Input className="form-input-borderless"
                      placeholder="20-digit hexadecimal number"
                      name="clientId"
                      value={this.getApplicationClientId()}
                      onChange={this.setApplicationClientId}
                    />
                  </div>
                  <div className="column column-two-thirds">
                    <h5>Client secret</h5>
                    <Input className="form-input-borderless"
                      placeholder="40-digit hexadecimal number"
                      name="clientSecret"
                      value={this.getApplicationClientSecret()}
                      onChange={this.setApplicationClientSecret}
                    />
                  </div>
                </div>
              </div>

              <hr className="mvxxxl" />

              <div className="mvm">
                <h4 className="mbn position-relative">
                  <span className="position-hanging-indent">4.</span>
                  <span>Set the scope to specify the kind of access to {this.getApplicationApiName()} data you want.</span>
                </h4>
                <p className="type-s">
                  <span>Use the <a href="https://developer.github.com/v3/oauth/#scopes">scope documentation at GitHub</a> to determine </span>
                  <span>the correct value for your application.</span>
                </p>

                <div className="columns">
                  <div className="column column-one-third">
                    <Input className="form-input-borderless"
                      name="scope"
                      value={this.getApplicationScope()}
                      onChange={this.setApplicationScope}
                      placeholder="Enter scope value"
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
