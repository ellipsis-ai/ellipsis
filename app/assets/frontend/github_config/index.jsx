// @flow
import * as React from 'react';
import CSRFTokenHiddenInput from '../shared_ui/csrf_token_hidden_input';
import PageComponent from '../shared_ui/page';
const Page: any = PageComponent;
import SettingsPage from '../shared_ui/settings_page';
import type {PageRequiredProps} from "../shared_ui/page";
import autobind from '../lib/autobind';

const resetForm = jsRoutes.controllers.GithubConfigController.reset();

export type GithubConfigProps = {
  isAdmin: boolean,
  csrfToken: string,
  teamId: string,
  linkedAccount: ?{
    providerId: string,
    providerKey: string,
    createdAt: string
  }
}

type Props = GithubConfigProps & PageRequiredProps

class GithubConfig extends React.Component<Props> {
  props: Props;
  static defaultProps: PageRequiredProps;

    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    getLinkedAccount() {
      return this.props.linkedAccount;
    }

    render() {
      return (
        <SettingsPage teamId={this.props.teamId} isAdmin={this.props.isAdmin} header={"GitHub Configuration"} activePage={"githubConfig"}>
          {this.getLinkedAccount() ? this.renderLinkedAccount() : this.renderNoLinkedAccount()}
          {this.props.onRenderFooter()}
        </SettingsPage>
      );
    }

    renderLogo() {
      return (
        <img height="32" src="/assets/images/logos/GitHub-Mark-64px.png"/>
      );
    }

    renderLinkedAccount() {
      return (
        <div>
          <div className="columns">
            <div className="column">
              {this.renderLogo()}
            </div>
            <div className="column">
              <span>You have linked to your GitHub account</span>
            </div>
            <div className="column">
              <form action={resetForm.url} method={resetForm.method}>
                <CSRFTokenHiddenInput value={this.props.csrfToken} />
                <button type="submit" className="button-s button-shrink">Reset</button>
              </form>
            </div>
          </div>
        </div>
      );
    }

    getGithubAuthUrl() {
      const redirect = jsRoutes.controllers.GithubConfigController.index().url;
      return jsRoutes.controllers.SocialAuthController.authenticateGithub(redirect).url;
    }

    renderNoLinkedAccount() {
      return (
        <div className="columns">
          <div className="column">
            {this.renderLogo()}
          </div>
          <div className="column align-m">
            <span>To push code to or pull code from GitHub, you first need to </span>
            <a href={this.getGithubAuthUrl()}>authenticate your GitHub account</a>
          </div>
        </div>
      );
    }
}

GithubConfig.defaultProps = Page.requiredPropDefaults();

export default GithubConfig;
