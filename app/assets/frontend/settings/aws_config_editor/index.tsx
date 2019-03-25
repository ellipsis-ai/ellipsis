import * as React from 'react';
import Collapsible from '../../shared_ui/collapsible';
import CsrfTokenHiddenInput from '../../shared_ui/csrf_token_hidden_input';
import SettingsPage from '../../shared_ui/settings_page';
import FormInput from '../../form/input';
import {NavItemContent, PageRequiredProps} from '../../shared_ui/page';
import autobind from "../../lib/autobind";

export interface AwsConfigEditorProps {
  configId: string
  name?: Option<string>
  requiredNameInCode?: string
  accessKeyId?: Option<string>
  secretAccessKey?: Option<string>
  region?: Option<string>
  configSaved: boolean
  csrfToken: string
  teamId: string
  behaviorGroupId?: Option<string>
  behaviorId?: Option<string>
  documentationUrl: string
  isAdmin: boolean
}

type Props = PageRequiredProps & AwsConfigEditorProps;

interface State {
  name: string
  accessKeyId: string
  secretAccessKey: string
  region: string
  hasName: boolean
  isSaving: boolean
}

class AwsConfigEditor extends React.Component<Props, State> {
    configNameInput: Option<FormInput>;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.state = {
        name: this.props.name || "",
        accessKeyId: this.props.accessKeyId || "",
        secretAccessKey: this.props.secretAccessKey || "",
        region: this.props.region || "",
        hasName: this.props.configSaved || false,
        isSaving: false
      };
    }

    componentDidMount(): void {
      this.renderNav();
    }

    componentDidUpdate(): void {
      this.renderNav();
    }

    getName(): string {
      return this.state.name;
    }

    nameIsEmpty(): boolean {
      return !this.getName();
    }

    setName(name: string): void {
      this.setState({ name: name });
    }

    onNameEnterKey(): void {
      if (!this.nameIsEmpty()) {
        if (this.configNameInput) {
          this.configNameInput.blur();
        }
      }
    }

    getAccessKeyId(): string {
      return this.state.accessKeyId;
    }

    setAccessKeyId(value: string): void {
      this.setState({ accessKeyId: value });
    }

    getSecretAccessKey(): string {
      return this.state.secretAccessKey;
    }

    setSecretAccessKey(value: string): void {
      this.setState({ secretAccessKey: value });
    }

    getRegion(): string {
      return this.state.region;
    }

    setRegion(value: string): void {
      this.setState({ region: value });
    }

    canBeSaved(): boolean {
      return Boolean(
        this.getName() && this.getAccessKeyId() && this.getSecretAccessKey() && this.getRegion()
      );
    }

    onSaveClick(): void {
      this.setState({
        isSaving: true
      });
    }

    reset() {
      this.setState({
        name: "",
        accessKeyId: "",
        secretAccessKey: "",
        region: "",
        hasName: false
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
        <SettingsPage teamId={this.props.teamId} activePage={"oauthApplications"} isAdmin={this.props.isAdmin}>
          <form action={jsRoutes.controllers.web.settings.AWSConfigController.save().url} method="POST" className="flex-row-cascade">
            <CsrfTokenHiddenInput value={this.props.csrfToken} />
            <input type="hidden" name="id" value={this.props.configId} />
            <input type="hidden" name="teamId" value={this.props.teamId} />
            <input type="hidden" name="requiredNameInCode" value={this.props.requiredNameInCode} />
            {this.renderBehaviorGroupId()}
            {this.renderBehaviorId()}

            {this.renderConfigure()}

            {this.props.onRenderFooter((
              <div className="container container-wide border-top prn">
                <div className="columns mobile-columns-float">
                  <div className="column column-one-quarter" />
                  <div className="column column-three-quarters phxxxxl ptm prm">
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
            ))}
          </form>
        </SettingsPage>
      );
    }

    renderNav() {
      const navItems: Array<NavItemContent> = [{
        title: "Settings"
      }, {
        url: jsRoutes.controllers.web.settings.IntegrationsController.list(this.props.isAdmin ? this.props.teamId : null).url,
        title: "Integrations"
      }];
      this.props.onRenderNavItems(navItems.concat(this.renderConfigNavItems()));
    }

    renderConfigNavItems(): Array<NavItemContent> {
      if (!this.props.configSaved) {
        return [{
          title: "Add AWS configuration"
        }];
      } else {
        const configName = this.getName() || "Untitled configuration";
        const title = configName === "AWS" ? "AWS" : `${configName} (AWS)`;
        return [{
          title: title
        }];
      }
    }

    renderConfigure() {
      return (
        <div>
          <p className="mtm mbxl">Set up a new AWS configuration so your skills can access data from an AWS account.</p>

          <div>
            <h4 className="mbn position-relative">
              <span className="position-hanging-indent">1</span>
              <span> Enter a name for this configuration</span>
            </h4>
            <p className="type-s">The name should help differentiate this from any other AWS configurations you may have with different kinds of access, or access to a different set of data. (e.g. Production, Staging, Readonly)</p>

            <div className="mbxxl columns">
              <div className="column column-two-thirds">
                <div>
                  <FormInput
                    ref={(el) => this.configNameInput = el}
                    name="name"
                    value={this.getName()}
                    placeholder={"e.g. Default"}
                    className="form-input-borderless form-input-l type-l"
                    onChange={this.setName}
                    onEnterKey={this.onNameEnterKey}
                  />
                </div>
              </div>
            </div>


            <Collapsible revealWhen={!this.nameIsEmpty()}>

              <div className="mvm">
                <h4 className="mbn position-relative">
                  <span className="position-hanging-indent">2</span>
                  <span>Ensure that you have set up a profile on your AWS account. </span>
                  {this.props.documentationUrl ? (
                    <a href={this.props.documentationUrl} target="_blank">Go to AWS ↗︎</a>
                  ) : null}
                </h4>
              </div>

              <hr className="mvxxxl" />

              <div className="mvm">
                <h4 className="mbn position-relative">
                  <span className="position-hanging-indent">3</span>
                  <span>Enter the configuration details</span>
                </h4>
                <p className="type-s">
                  The access keys will be generated by AWS after you’ve set up a profile.
                </p>

                <div className="mvm">
                  <div className="columns mtl">
                    <div className="column column-one-half">
                      <h5>Access Key ID</h5>
                      <FormInput className="form-input-borderless type-monospace"
                             placeholder="Enter access key ID"
                             name="accessKeyId"
                             value={this.getAccessKeyId()}
                             onChange={this.setAccessKeyId}
                             disableAuto={true}
                      />
                    </div>
                  </div>
                </div>
                <div className="mvm">
                  <div className="columns mtl">
                  <div className="column column-one-half">
                    <h5>Secret Access Key</h5>
                    <FormInput className="form-input-borderless type-monospace"
                           placeholder="Enter secret access key"
                           name="secretAccessKey"
                           value={this.getSecretAccessKey()}
                           onChange={this.setSecretAccessKey}
                           disableAuto={true}
                    />
                  </div>
                </div>
              </div>

              <div className="mvm">
                <div className="columns mtl">
                  <div className="column column-one-half">
                    <h5>Region</h5>
                    <FormInput className="form-input-borderless type-monospace"
                           placeholder="Enter the region"
                           name="region"
                           value={this.getRegion()}
                           onChange={this.setRegion}
                           disableAuto={true}
                    />
                  </div>
                </div>
              </div>
            </div>

            <hr className="mvxxxl" />

            </Collapsible>
          </div>
        </div>
      );
    }
}

export default AwsConfigEditor;
