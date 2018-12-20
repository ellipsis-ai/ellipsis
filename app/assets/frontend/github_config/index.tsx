import * as React from 'react';
import CSRFTokenHiddenInput from '../shared_ui/csrf_token_hidden_input';
import SettingsPage from '../shared_ui/settings_page';
import {PageRequiredProps} from "../shared_ui/page";
import autobind from '../lib/autobind';

interface LinkedAccount {
  providerId: string,
  providerKey: string,
  createdAt: string
}

export interface GithubConfigProps {
  isAdmin: boolean,
  csrfToken: string,
  teamId: string,
  linkedAccount: Option<LinkedAccount>
}

type Props = GithubConfigProps & PageRequiredProps

class GithubConfig extends React.Component<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    getLinkedAccount() {
      return this.props.linkedAccount;
    }

    render() {
      return (
        <SettingsPage teamId={this.props.teamId} isAdmin={this.props.isAdmin} activePage={"githubConfig"}>
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
      const resetAction = jsRoutes.controllers.GithubConfigController.reset();
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
              <form action={resetAction.url} method={resetAction.method}>
                <CSRFTokenHiddenInput value={this.props.csrfToken} />
                {this.props.isAdmin ? (
                  <input type="hidden" name="teamId" value={this.props.teamId} />
                ) : null}
                <button type="submit" className="button-s button-shrink">Reset</button>
              </form>
            </div>
          </div>
        </div>
      );
    }

    getGithubAuthUrl() {
      const teamId = this.props.isAdmin ? this.props.teamId : null;
      const redirect = jsRoutes.controllers.GithubConfigController.index(teamId).url;
      return jsRoutes.controllers.SocialAuthController.authenticateGithub(redirect, teamId, null).url;
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

export default GithubConfig;
