import * as React from 'react';
import Checkbox from '../../form/checkbox';
import Collapsible from '../../shared_ui/collapsible';
import CsrfTokenHiddenInput from '../../shared_ui/csrf_token_hidden_input';
import FormInput from '../../form/input';
import SettingsPage from '../../shared_ui/settings_page';
import BrowserUtils from '../../lib/browser_utils';
import {NavItemContent, PageRequiredProps} from '../../shared_ui/page';
import {OAuthApiJson} from "../../models/oauth";
import User from "../../models/user";
import autobind from "../../lib/autobind";
import Button from "../../form/button";

export interface OAuthEditorProps {
  isAdmin: boolean,
  apis: Array<OAuthApiJson>
  oauth1CallbackUrl: string
  oauth2CallbackUrl: string
  authorizationUrl: string
  applicationKey?: string
  applicationSecret?: string
  requiresAuth?: boolean
  applicationApiId?: string
  recommendedScope?: string
  applicationScope?: string
  requiredNameInCode?: string
  applicationName?: string
  applicationSaved?: boolean
  applicationShared: boolean
  applicationCanBeShared: boolean
  csrfToken: string
  teamId: string
  mainUrl: string
  applicationId?: string,
  behaviorGroupId?: string,
  behaviorId?: string,
  sharedTokenUser?: User
}

type Props = OAuthEditorProps & PageRequiredProps;

interface State {
  applicationApi: Option<OAuthApiJson>
  applicationName: string
  applicationKey: string
  applicationSecret: string
  applicationScope: string
  hasNamedApplication: boolean
  shouldRevealApplicationUrl: boolean
  isSaving: boolean
  applicationShared: boolean,
  sharedTokenUserId?: string
}

class IntegrationEditor extends React.Component<Props, State> {
    applicationNameInput: Option<FormInput>;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.state = {
        applicationApi: this.findApiById(this.props.applicationApiId),
        applicationName: this.props.applicationName || "",
        applicationKey: this.props.applicationKey || "",
        applicationSecret: this.props.applicationSecret || "",
        applicationScope: this.props.applicationScope || this.props.recommendedScope || "",
        hasNamedApplication: this.props.applicationSaved || false,
        shouldRevealApplicationUrl: this.props.applicationSaved || false,
        isSaving: false,
        applicationShared: this.props.applicationShared,
        sharedTokenUserId: this.props.sharedTokenUser ? this.props.sharedTokenUser.ellipsisUserId : undefined
      };
    }

    getAllApis(): Array<OAuthApiJson> {
      return this.props.apis;
    }

    findApiById(id: Option<string>): Option<OAuthApiJson> {
      if (id) {
        const matches = this.getAllApis().filter(function(api) { return api.apiId === id; });
        if (matches.length > 0) {
          return matches[0];
        }
      }
      return null;
    }

    getCallbackUrl(): string {
      if (this.isOAuth1()) {
        return this.props.oauth1CallbackUrl;
      } else {
        return this.props.oauth2CallbackUrl;
      }
    }

    getMainUrl(): string {
      return this.props.mainUrl;
    }

    apiIsSet(): boolean {
      return Boolean(this.state.applicationApi);
    }

    getApplicationApiName(): string {
      return this.state.applicationApi ? this.state.applicationApi.name : "";
    }

    getApplicationApiScopeDocumentationUrl(): Option<string> {
      return this.state.applicationApi ? this.state.applicationApi.scopeDocumentationUrl : null;
    }

    getApplicationApiNewApplicationUrl(): Option<string> {
      return this.state.applicationApi ? this.state.applicationApi.newApplicationUrl : null;
    }

    getApplicationApiId(): string {
      return this.state.applicationApi ? this.state.applicationApi.apiId : "";
    }

    setApplicationApi(api: OAuthApiJson): void {
      this.setState({ applicationApi: api }, () => {
        if (this.applicationNameInput) {
          this.applicationNameInput.focus();
        }
        BrowserUtils.modifyQueryParam('apiId', api.apiId);
      });
    }

    reset(): void {
      this.setState({
        applicationApi: null,
        applicationName: "",
        applicationScope: "",
        hasNamedApplication: false,
        shouldRevealApplicationUrl: false,
        applicationShared: false,
        sharedTokenUserId: undefined
      }, function() {
        BrowserUtils.removeQueryParam('apiId');
      });
    }

    getApplicationName(): string {
      return this.state.applicationName;
    }

    applicationNameIsEmpty(): boolean {
      return !this.getApplicationName();
    }

    setApplicationName(name: string): void {
      this.setState({ applicationName: name });
    }

    onApplicationNameEnterKey(): void {
      if (!this.applicationNameIsEmpty()) {
        this.revealApplicationURL();
        if (this.applicationNameInput) {
          this.applicationNameInput.blur();
        }
      }
    }

    revealApplicationURL(): void {
      this.setState({ shouldRevealApplicationUrl: true });
    }

    shouldRevealApplicationUrl(): boolean {
      return this.state.shouldRevealApplicationUrl;
    }

    shouldRevealCallbackUrl(): boolean {
      return this.props.requiresAuth || Boolean(this.state.applicationApi && this.state.applicationApi.requiresAuth);
    }

    getApplicationKey(): string {
      return this.state.applicationKey;
    }

    setApplicationKey(value: string): void {
      this.setState({ applicationKey: value });
    }

    getApplicationSecret(): string {
      return this.state.applicationSecret;
    }

    setApplicationSecret(value: string): void {
      this.setState({ applicationSecret: value });
    }

    getApplicationScope(): string {
      return this.state.applicationScope;
    }

    setApplicationScope(value: string): void {
      this.setState({ applicationScope: value });
    }

    oauthDetailsCanBeSaved(): boolean {
      return Boolean(this.getApplicationKey() && this.getApplicationSecret());
    }

    canBeSaved(): boolean {
      return Boolean(
        this.getApplicationApiName() && this.getApplicationName() && this.oauthDetailsCanBeSaved()
      );
    }

    applicationCanBeShared(): boolean {
      return this.props.applicationCanBeShared;
    }

    onSaveClick(): void {
      this.setState({
        isSaving: true
      });
    }

    onFocusExample(event: React.FocusEvent<HTMLInputElement>): void {
      if (event) {
        event.currentTarget.select();
      }
    }

    onApplicationSharedChange(isChecked: boolean): void {
      this.setState({
        applicationShared: isChecked
      });
    }

    renderBehaviorGroupId() {
      var id = this.props.behaviorGroupId;
      if (id && id.length > 0) {
        return (<input type="hidden" name="behaviorGroupId" value={id} />);
      } else {
        return null;
      }
    }

    renderBehaviorId() {
      var id = this.props.behaviorId;
      if (id && id.length > 0) {
        return (<input type="hidden" name="behaviorId" value={id} />);
      } else {
        return null;
      }
    }

    render() {
      return (
        <SettingsPage teamId={this.props.teamId} isAdmin={this.props.isAdmin} activePage={"oauthApplications"}>
          <form action={jsRoutes.controllers.web.settings.IntegrationsController.save().url} method="POST" className="flex-row-cascade">
            <CsrfTokenHiddenInput value={this.props.csrfToken} />
            <input type="hidden" name="apiId" value={this.getApplicationApiId()} />
            <input type="hidden" name="requiredNameInCode" value={this.props.requiredNameInCode} />
            <input type="hidden" name="id" value={this.props.applicationId} />
            <input type="hidden" name="teamId" value={this.props.teamId} />
            <input type="hidden" name="isForOAuth1" value={String(this.isOAuth1())} />
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
          {this.renderNav()}
        </SettingsPage>
      );
    }

    renderNav() {
      return this.props.onRenderNavItems(this.renderApplicationNavItems());
    }

    renderApplicationNavItems(): Array<NavItemContent> {
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
    }

    redirectToAWSEditor(): void {
      window.location.href = jsRoutes.controllers.web.settings.AWSConfigController.add(null, null, null, null).url;
    }

    renderChooseApi() {
      return (
        <div>
          <p className="mtm">
            <span>Choose an API you would like to integrate with Ellipsis. This will allow your skills to read </span>
            <span>and/or write data from that product. You can create multiple configurations for a single API, each </span>
            <span>with a different level of access.</span>
          </p>

          <div className="mvxl">
            <Button className="button-l mrl mbl" onClick={this.redirectToAWSEditor}>
              <span className="type-black">AWS</span>
            </Button>
            {this.getAllApis().map((api, index) => (
              <Button key={"apiTypeButton" + index}
                      className="button-l mrl mbl"
                      onClick={this.setApplicationApi.bind(this, api)}
              >
                {api.logoImageUrl ? (
                  <img alt="" src={api.logoImageUrl} height="32" className="align-m" />
                ) : (
                  <span>
                    {api.iconImageUrl ? (
                      <img alt="" src={api.iconImageUrl} width="24" height="24" className="mrm align-m mbxs" />
                    ) : null}
                    <span className="type-black">{api.name}</span>
                  </span>
                )}
              </Button>
            ))}
          </div>
        </div>
      );
    }

    renderCallbackUrl() {
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
    }

    renderConfigureApplication() {
      return (
        <div>
          <p className="mtm mbxl">Set up a new {this.getApplicationApiName()} configuration so your skills can access data from a {this.getApplicationApiName()} account.</p>

          <div>
            {this.renderConfigureApplicationName()}
            {this.renderConfigureApplicationDetails()}
          </div>
        </div>
      );
    }

    renderConfigureApplicationName() {
      return (
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
        </div>
      );
    }

    isOAuth1(): boolean {
      return Boolean(this.state.applicationApi && this.state.applicationApi.isOAuth1);
    }

    renderOAuthDetails() {
      return (
        <div>
          <hr className="mvxxxl" />

          <div className="mvm">
            <h4 className="mbn position-relative">
              <span className="position-hanging-indent">3</span>
              <span>Paste the key and secret from your {this.getApplicationApiName()} OAuth application</span>
            </h4>
            <p className="type-s">
              These values will be generated by {this.getApplicationApiName()} after you’ve saved an application there in step 2.
            </p>

            <div className="columns mtl">
              <div className="column column-one-half">
                <h5 className="mtn">Consumer key or client ID</h5>
                <FormInput className="form-input-borderless type-monospace"
                           placeholder="Enter key"
                           name="key"
                           value={this.getApplicationKey()}
                           onChange={this.setApplicationKey}
                           disableAuto={true}
                />
              </div>
              <div className="column column-one-half">
                <h5 className="mtn">Consumer or client secret</h5>
                <FormInput className="form-input-borderless type-monospace"
                           placeholder="Enter secret"
                           name="secret"
                           value={this.getApplicationSecret()}
                           onChange={this.setApplicationSecret}
                           disableAuto={true}
                />
              </div>
            </div>
          </div>
        </div>
      );
    }

    renderScopeDetails() {
      const docUrl = this.getApplicationApiScopeDocumentationUrl();
      return (
        <div>
          <hr className="mvxxxl" />

          <div className="mvm">
            <h4 className="mbn position-relative">
              <span className="position-hanging-indent">4</span>
              <span>Set the scope to specify the kind of access to {this.getApplicationApiName()} data you want.</span>
            </h4>
            <p className="type-s">
              This may not be necessary for some APIs.
            </p>
            {docUrl ? (
              <p className="type-s">
                <span>Use the <a href={docUrl} target="_blank">scope documentation at {this.getApplicationApiName()}</a> to determine </span>
                <span>the correct value for your configuration.</span>
              </p>
            ) : null}

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
        </div>
      );
    }

    resetSharedTokenUserId() {
      this.setState({
        sharedTokenUserId: undefined
      });
    }

    renderSharedTokenUser() {
      const sharedTokenUserName = this.props.sharedTokenUser ? this.props.sharedTokenUser.fullName : undefined;
      if (this.props.sharedTokenUser && this.state.sharedTokenUserId) {
        return (
          <div>
            <div>
              <input type="hidden" name="sharedTokenUserId" value={this.state.sharedTokenUserId} />
              <span>Token provided by {sharedTokenUserName} is shared for all users on the team.</span>
            </div>
            <div>
              <Button
                onClick={this.resetSharedTokenUserId}
                className="button-s button-shrink">Reset</Button>
            </div>
          </div>
        );
      } else {
        return (
          <div>
            <div>
              <span>No sharing: each individual user must authorize the bot.</span>
            </div>
            <div>
              <a href={this.props.authorizationUrl}>Authorize and share your token</a>
            </div>
          </div>
        );
      }
    }

    renderSharedTokenDetails() {
      return (
        <div className="mvm">
          <h4 className="mbn position-relative">
            <span className="position-hanging-indent">5</span>
            <span>Token sharing</span>
          </h4>
          {this.renderSharedTokenUser()}
        </div>
      );
    }

    renderConfigureApplicationDetails() {
      const newAppUrl = this.getApplicationApiNewApplicationUrl();
      return (
        <div>
          <Collapsible revealWhen={this.shouldRevealApplicationUrl()}>
            <hr className="mvxxxl" />

            <div className="mvm">
              <h4 className="mbn position-relative">
                <span className="position-hanging-indent">2</span>
                <span>Register a new OAuth developer application on your {this.getApplicationApiName()} account. </span>
                {newAppUrl ? (
                  <a href={newAppUrl} target="_blank">Go to {this.getApplicationApiName()} ↗︎</a>
                ) : null}
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

            {this.renderOAuthDetails()}

            {this.renderScopeDetails()}

            {this.renderSharedTokenDetails()}

            {this.applicationCanBeShared() ? (
              <div className="mvm">
                <h4 className="mbn position-relative">
                  <span className="position-hanging-indent">6</span>
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
            ) : null}

          </Collapsible>
        </div>
      );
    }
}

export default IntegrationEditor;
