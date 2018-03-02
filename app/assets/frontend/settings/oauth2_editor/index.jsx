import * as React from 'react';
import Checkbox from '../../form/checkbox';
import Collapsible from '../../shared_ui/collapsible';
import CsrfTokenHiddenInput from '../../shared_ui/csrf_token_hidden_input';
import FormInput from '../../form/input';
import SettingsPage from '../../shared_ui/settings_page';
import BrowserUtils from '../../lib/browser_utils';
import ifPresent from '../../lib/if_present';
import Page from '../../shared_ui/page';

const IntegrationEditor = React.createClass({
    propTypes: Object.assign({}, Page.requiredPropTypes, {
      isAdmin: React.PropTypes.bool.isRequired,
      apis: React.PropTypes.arrayOf(React.PropTypes.shape({
        apiId: React.PropTypes.string.isRequired,
        name: React.PropTypes.string.isRequired,
        requiresAuth: React.PropTypes.bool.isRequired,
        newApplicationUrl: React.PropTypes.string,
        scopeDocumentationUrl: React.PropTypes.string,
        iconImageUrl: React.PropTypes.string,
        logoImageUrl: React.PropTypes.string
      })).isRequired,
      applicationApiId: React.PropTypes.string,
      recommendedScope: React.PropTypes.string,
      requiredNameInCode: React.PropTypes.string,
      requiredOAuth2ApiConfigId: React.PropTypes.string,
      applicationName: React.PropTypes.string,
      requiresAuth: React.PropTypes.bool,
      applicationClientId: React.PropTypes.string,
      applicationClientSecret: React.PropTypes.string,
      applicationScope: React.PropTypes.string,
      applicationSaved: React.PropTypes.bool,
      applicationShared: React.PropTypes.bool.isRequired,
      applicationCanBeShared: React.PropTypes.bool.isRequired,
      csrfToken: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired,
      callbackUrl: React.PropTypes.string.isRequired,
      mainUrl: React.PropTypes.string.isRequired,
      applicationId: React.PropTypes.string,
      behaviorGroupId: React.PropTypes.string,
      behaviorId: React.PropTypes.string
    }),

    componentDidMount: function() {
      this.renderNav();
    },

    componentDidUpdate: function() {
      this.renderNav();
    },

    applicationNameInput: null,

    getDefaultProps: function() {
      return Page.requiredPropDefaults();
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
        isSaving: false,
        applicationShared: this.props.applicationShared
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
        if (this.applicationNameInput) {
          this.applicationNameInput.focus();
        }
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
        shouldRevealApplicationUrl: false,
        applicationShared: false
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
        if (this.applicationNameInput) {
          this.applicationNameInput.blur();
        }
      }
    },

    revealApplicationURL: function() {
      this.setState({ shouldRevealApplicationUrl: true });
    },

    shouldRevealApplicationUrl: function() {
      return this.state.shouldRevealApplicationUrl;
    },

    shouldRevealCallbackUrl: function() {
      return this.props.requiresAuth || !!(this.state.applicationApi && this.state.applicationApi.requiresAuth);
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

    applicationCanBeShared: function() {
      return this.props.applicationCanBeShared;
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

    onApplicationSharedChange: function(isChecked) {
      this.setState({
        applicationShared: isChecked
      });
    },

    renderBehaviorGroupId: function() {
      var id = this.props.behaviorGroupId;
      if (id && id.length > 0) {
        return (<input type="hidden" name="behaviorGroupId" value={id} />);
      } else {
        return null;
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
        <SettingsPage teamId={this.props.teamId} isAdmin={this.props.isAdmin} activePage={"oauthApplications"}>
          <form action={jsRoutes.controllers.web.settings.OAuth2ApplicationController.save().url} method="POST" className="flex-row-cascade">
            <CsrfTokenHiddenInput value={this.props.csrfToken} />
            <input type="hidden" name="apiId" value={this.getApplicationApiId()} />
            <input type="hidden" name="requiredNameInCode" value={this.props.requiredNameInCode} />
            <input type="hidden" name="id" value={this.props.applicationId} />
            <input type="hidden" name="teamId" value={this.props.teamId} />
            {this.renderBehaviorGroupId()}
            {this.renderBehaviorId()}

            <Collapsible revealWhen={!this.apiIsSet()}>
              {this.renderChooseApi()}
            </Collapsible>
            <Collapsible revealWhen={this.apiIsSet()}>
              {this.renderConfigureApplication()}
            </Collapsible>

            {this.props.onRenderFooter(
              <div className="container container-wide prn border-top">
                <div className="columns mobile-columns-float">
                  <div className="column column-one-quarter" />
                  <div className="column column-three-quarters phxxxxl ptm">
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
            )}
          </form>
        </SettingsPage>
      );
    },

    renderNav: function() {
      const navItems = [{
        title: "Settings"
      }, {
        url: jsRoutes.controllers.web.settings.IntegrationsController.list(this.props.isAdmin ? this.props.teamId : null).url,
        title: "Integrations"
      }];
      this.props.onRenderNavItems(navItems.concat(this.renderApplicationNavItems()));
    },

    renderApplicationNavItems: function() {
      const apiName = this.getApplicationApiName();
      if (!this.apiIsSet()) {
        return [{
          title: "Add configuration"
        }];
      } else if (!this.props.applicationSaved) {
        return [{
          callback: this.reset,
          title: `Add ${apiName} configuration`
        }];
      } else {
        const configName = this.getApplicationName() || "Untitled configuration";
        const title = configName === apiName ? apiName : `${configName} (${apiName})`;
        return [{
          title: title
        }];
      }
    },

    redirectToAWSEditor: function() {
      window.location = jsRoutes.controllers.web.settings.AWSConfigController.add().url;
    },

    renderChooseApi: function() {
      return (
        <div>
          <p className="mtm">
            <span>Choose an API you would like to integrate with Ellipsis. This will allow your skills to read </span>
            <span>and/or write data from that product. You can create multiple configurations for a single API, each </span>
            <span>with a different level of access.</span>
          </p>

          <div className="mvxl">
            <button type="button" key={"apiTypeButton12"} className="button-l mrl mbl" onClick={this.redirectToAWSEditor}>
              <span className="type-black">AWS</span>
          </button>
            {this.props.apis.map((api, index) => (
              <button type="button" key={"apiTypeButton" + index}
                      className="button-l mrl mbl"
                      onClick={this.setApplicationApi.bind(this, api)}
              >
                {ifPresent(api.logoImageUrl, url => (
                  <img src={url} height="32" className="align-m" />
                ), () => (
                  <span>
                    {ifPresent(api.iconImageUrl, url => (
                      <img src={url} width="24" height="24" className="mrm align-m mbxs" />
                    ))}
                    <span className="type-black">{api.name}</span>
                  </span>
                ))}
              </button>
            ))}
          </div>
        </div>
      );
    },

    renderCallbackUrl: function() {
      if (this.shouldRevealCallbackUrl()) {
        return (
          <li>
            <div>Copy and paste this for the <b>callback URL</b> (sometimes called <b>redirect URL</b>):</div>
            <input type="text" readOnly={true} className="box-code-example display-ellipsis mtl"
                   value={this.getCallbackUrl()} onFocus={this.onFocusExample}/>
          </li>
        );
      } else {
        return null;
      }
    },

    renderConfigureApplication: function() {
      return (
        <div>
          <p className="mtm mbxl">Set up a new {this.getApplicationApiName()} configuration so your skills can access data from a {this.getApplicationApiName()} account.</p>

          <div>
            <h4 className="mbn position-relative">
              <span className="position-hanging-indent">1</span>
              <span> Enter a name for this configuration</span>
            </h4>
            <p className="type-s">
              <span>If you have multiple configurations for {this.getApplicationApiName()}, e.g. with different scopes, </span>
              <span>the name should differentiate this one from others.</span>
            </p>

            <div className="mbxxl columns">
              <div className="column column-two-thirds">
                <div>
                  <FormInput
                    ref={(el) => this.applicationNameInput = el}
                    name="name"
                    value={this.getApplicationName()}
                    placeholder={"e.g. " + this.getApplicationApiName()}
                    className="form-input-borderless form-input-l type-l"
                    onChange={this.setApplicationName}
                    onEnterKey={this.onApplicationNameEnterKey}
                  />
                </div>

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
                  {this.renderCallbackUrl()}
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
                    <h5 className="mtn">Client ID</h5>
                    <FormInput className="form-input-borderless type-monospace"
                           placeholder="Enter identifier"
                           name="clientId"
                           value={this.getApplicationClientId()}
                           onChange={this.setApplicationClientId}
                           disableAuto={true}
                    />
                  </div>
                  <div className="column column-one-half">
                    <h5 className="mtn">Client secret</h5>
                    <FormInput className="form-input-borderless type-monospace"
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
                <p className="type-s">
                  This may not be necessary for some APIs.
                </p>
                {ifPresent(this.getApplicationApiScopeDocumentationUrl(), url => (
                  <p className="type-s">
                    <span>Use the <a href={url} target="_blank">scope documentation at {this.getApplicationApiName()}</a> to determine </span>
                    <span>the correct value for your configuration.</span>
                  </p>
                ))}

                <div className="columns">
                  <div className="column column-one-third">
                    <FormInput className="form-input-borderless type-monospace"
                           name="scope"
                           value={this.getApplicationScope()}
                           onChange={this.setApplicationScope}
                           placeholder="Enter scope value"
                           disableAuto={true}
                    />
                  </div>
                </div>
              </div>

              {ifPresent(this.applicationCanBeShared(), () => (
                <div className="mvm">
                  <h4 className="mbn position-relative">
                    <span className="position-hanging-indent">5</span>
                    <span>Optionally share {this.getApplicationApiName()} with other teams.</span>
                  </h4>
                  <p className="type-s">
                    This option is available for Ellipsis admins only.
                  </p>

                  <div className="columns">
                    <div className="column column-one-third">
                      <Checkbox
                        className="display-block type-s"
                        onChange={this.onApplicationSharedChange}
                        checked={this.state.applicationShared}
                        label="Shared with other teams"
                        name="isShared"
                      />
                    </div>
                  </div>
                </div>
              ))}

            </Collapsible>
          </div>
        </div>
      );
    }
  });

export default IntegrationEditor;
