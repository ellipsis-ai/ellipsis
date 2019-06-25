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
import DynamicLabelButton from "../../form/dynamic_label_button";
import SVGCheckmark from "../../svg/checkmark";
import ConfirmActionPanel from "../../panels/confirm_action";

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
  isDeleting: boolean
  applicationShared: boolean,
  sharedTokenUserId: string
  revealEditor: boolean
}

class IntegrationEditor extends React.Component<Props, State> {
  private applicationNameInput: Option<FormInput>;
  private editForm: Option<HTMLFormElement>;
  private deleteForm: Option<HTMLFormElement>;

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
        isDeleting: false,
        applicationShared: this.props.applicationShared,
        sharedTokenUserId: this.props.sharedTokenUser ? this.props.sharedTokenUser.ellipsisUserId : "",
        revealEditor: !this.props.applicationSaved
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
        sharedTokenUserId: ""
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
      const hasRequiredElements = Boolean(this.getApplicationApiName() && this.getApplicationName() && this.oauthDetailsCanBeSaved());
      return hasRequiredElements && this.hasChanges();
    }

    hasChanges(): boolean {
      return this.state.applicationName !== this.props.applicationName ||
        this.state.applicationKey !== this.props.applicationKey ||
        this.state.applicationSecret !== this.props.applicationSecret ||
        this.state.applicationScope !== this.props.applicationScope ||
        this.state.applicationShared !== this.props.applicationShared;
    }

    applicationCanBeShared(): boolean {
      return this.props.applicationCanBeShared;
    }

    onSaveClick(): void {
      this.setState({
        isSaving: true
      }, () => {
        if (this.editForm) {
          this.editForm.submit();
        }
      });
    }

    cancelEditMode(): void {
      this.toggleEditMode();
      this.setState({
        applicationName: this.props.applicationName || "",
        applicationKey: this.props.applicationKey || "",
        applicationSecret: this.props.applicationSecret || "",
        applicationScope: this.props.applicationScope || "",
        applicationShared: this.props.applicationShared
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
          <form ref={(el) => this.editForm = el} action={jsRoutes.controllers.web.settings.IntegrationsController.save().url} method="POST" className="flex-row-cascade">
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

          </form>
          {this.props.onRenderFooter(
            <div>
              <Collapsible revealWhen={this.props.activePanelName === "confirmDeleteIntegration"}>
                <ConfirmActionPanel
                  onCancelClick={this.toggleConfirmDelete}
                  onConfirmClick={this.doConfirmDelete}
                  confirmText={"Remove"}
                  confirmingText={"Removing…"}
                  isConfirming={this.state.isDeleting}
                >
                  <div>Are you sure you want to remove the <b>{this.getApplicationName()}</b> integration?</div>
                </ConfirmActionPanel>
              </Collapsible>
              <Collapsible revealWhen={this.state.revealEditor}>
                <div className="container container-wide prn border-top">
                  <div className="columns mobile-columns-float">
                    <div className="column column-one-quarter" />
                    <div className="column column-three-quarters phxxxxl ptm">
                      <DynamicLabelButton
                        onClick={this.onSaveClick}
                        className="button-primary mrs mbm"
                        disabledWhen={!this.canBeSaved()}
                        labels={[{
                          text: "Save changes",
                          displayWhen: !this.state.isSaving
                        }, {
                          text: "Saving…",
                          displayWhen: this.state.isSaving
                        }]}
                      />
                      {this.props.applicationSaved ? (
                        <Button className="mbm" onClick={this.cancelEditMode}>Cancel</Button>
                      ) : null}
                    </div>
                  </div>
                </div>
              </Collapsible>
            </div>
          )}
          {this.renderNav()}
          <form ref={(el) => this.deleteForm = el} action={jsRoutes.controllers.web.settings.IntegrationsController.delete().url} method="post">
            <CsrfTokenHiddenInput value={this.props.csrfToken} />
            <input type="hidden" name="id" value={this.props.applicationId} />
            <input type="hidden" name="teamId" value={this.props.teamId} />
          </form>
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
        const configName = this.props.applicationName || "Untitled integration";
        return [{
          title: this.appNameWithOptionalApiName(configName)
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

    renderCallbackUrlInput() {
      return (
        <input type="text" readOnly={true} className="box-code-example display-ellipsis"
          value={this.getCallbackUrl()} onFocus={this.onFocusExample} />
      );
    }

    renderCallbackUrl() {
      if (this.shouldRevealCallbackUrl()) {
        return (
          <li>
            <div>Copy and paste this for the <b>callback URL</b> (sometimes called <b>redirect URL</b>):</div>
            <div className="mtl">{this.renderCallbackUrlInput()}</div>
          </li>
        );
      } else {
        return null;
      }
    }

    renderConfigureApplication() {
      return (
        <div>
          <Collapsible revealWhen={!this.state.revealEditor}>
            {this.renderApplicationSummary()}
          </Collapsible>
          <Collapsible revealWhen={this.state.revealEditor}>
            <div>
              <p className="mtm mbxl">Set up a new {this.getApplicationApiName()} configuration so your skills can access
                data from a {this.getApplicationApiName()} account.</p>

              <div>
                {this.renderConfigureApplicationName()}
                {this.renderConfigureApplicationDetails()}
              </div>
            </div>
          </Collapsible>
        </div>
      );
    }

    toggleEditMode(): void {
      this.setState({
        revealEditor: !this.state.revealEditor
      });
    }

    toggleConfirmDelete(): void {
      this.props.onToggleActivePanel("confirmDeleteIntegration", true);
    }

    doConfirmDelete(): void {
      this.setState({
        isDeleting: true
      }, () => {
        if (this.deleteForm) {
          this.deleteForm.submit();
        }
      });
    }

    appNameWithOptionalApiName(appName: string) {
      const apiName = this.getApplicationApiName();
      if (appName.toLowerCase().includes(apiName.toLowerCase())) {
        return appName;
      } else {
        return `${appName} (${apiName})`;
      }
    }

    renderApplicationSummary() {
      return (
        <div>
          <h3>{this.appNameWithOptionalApiName(this.getApplicationName())}</h3>

          <h5>Callback URL</h5>
          <div>{this.renderCallbackUrlInput()}</div>

          <div className="columns">
            <div className="column column-one-third">
              <h5>Client ID</h5>
              <div className="display-ellipsis type-s type-monospace" title={this.getApplicationKey()}>{this.getApplicationKey()}</div>
            </div>
            <div className="column column-one-third">
              <h5>Client secret</h5>
              <div className="type-s type-disabled">•••••••••••••••••••••••</div>
            </div>
            <div className="column column-one-third">
              <h5>Scope</h5>
              <div className="display-ellipsis type-s type-monospace" title={this.getApplicationScope()}>{this.getApplicationScope()}</div>
            </div>
          </div>

          <input type="hidden" name="name" value={this.getApplicationName()} />
          <input type="hidden" name="key" value={this.getApplicationKey()} />
          <input type="hidden" name="secret" value={this.getApplicationSecret()} />
          <input type="hidden" name="scope" value={this.getApplicationScope()} />

          <div className="mvl">
            <Button className="mbs mrs" onClick={this.toggleEditMode}>Edit details</Button>
            <Button className="mbs mrs" onClick={this.toggleConfirmDelete}>Remove integration</Button>
          </div>

          {this.renderSharedTokenDetails()}

        </div>
      )
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
        sharedTokenUserId: "",
        isSaving: true
      }, () => {
        if (this.editForm) {
          this.editForm.submit();
        }
      });
    }

    renderSharedTokenUser() {
      const sharedTokenUserName = this.props.sharedTokenUser ? this.props.sharedTokenUser.fullName : undefined;
      if (this.props.sharedTokenUser && (this.state.sharedTokenUserId || this.state.isSaving)) {
        return (
          <div>
            <div>
              <input type="hidden" name="sharedTokenUserId" value={this.state.sharedTokenUserId} />
              <span className="display-inline-block mrs type-green height-xl align-b"><SVGCheckmark /></span>
              <b>Shared authorization: </b>
              <span>Authorized by {sharedTokenUserName} and access shared with the team.</span>
            </div>
            <div className="mtl type-s">
              <DynamicLabelButton
                className="button-s button-shrink align-m mrxs"
                onClick={this.resetSharedTokenUserId}
                disabledWhen={this.state.isSaving}
                labels={[{
                  text: "Remove shared authorization",
                  displayWhen: !this.state.isSaving
                }, {
                  text: "Removing…",
                  displayWhen: this.state.isSaving
                }]}
              />
              <span> — Switch to individual user authorization</span>
            </div>
          </div>
        );
      } else {
        return (
          <div>
            <input type="hidden" name="sharedTokenUserId" value={this.state.sharedTokenUserId} />
            <div>
              <b>Individual authorization: </b>
              <span>Each user must authorize individually with {this.getApplicationApiName()} to run any action using this integration.</span>
            </div>
            <div className="mtl type-s">
              <a
                className="button button-s button-shrink align-m mrxs"
                href={this.props.authorizationUrl}
              >Enable shared authorization</a> — Authorize with your credentials to share your access with the team
            </div>
          </div>
        );
      }
    }

    renderSharedTokenDetails() {
      if (this.props.authorizationUrl) {
        return (
          <div className="border-top mtxl ptxl">

            <h5 className="mtn">Authorization sharing</h5>

            {this.renderSharedTokenUser()}
          </div>
        );
      } else {
        return null;
      }
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
