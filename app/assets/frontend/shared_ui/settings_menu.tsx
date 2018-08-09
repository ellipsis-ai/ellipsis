import * as React from 'react';
import autobind from "../lib/autobind";

interface Props {
  teamId: string,
  activePage: Option<string>,
  isAdmin: boolean
}

class SettingsMenu extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  activeClassWhenPageName(pageName: string): string {
    return this.props.activePage === pageName ? "list-nav-active-item" : "";
  }

  render() {
    const teamId = this.props.isAdmin ? this.props.teamId : null;
    return (
      <nav className="mvxxl">
        <ul className="list-nav">
          <li className="mbxl">
            <h5>Team settings</h5>
          </li>
          <li className={this.activeClassWhenPageName("regionalSettings")}>
            <a href={jsRoutes.controllers.web.settings.RegionalSettingsController.index(teamId).url}>Regional
              settings</a>
          </li>
          <li className={this.activeClassWhenPageName("environmentVariables")}>
            <a href={jsRoutes.controllers.web.settings.EnvironmentVariablesController.list(teamId).url}>Environment
              variables</a>
          </li>
          <li className={this.activeClassWhenPageName("oauthApplications")}>
            <a href={jsRoutes.controllers.web.settings.IntegrationsController.list(teamId).url}>Integrations</a>
          </li>
          <li className="mtxxxl mbxl">
            <h5>User settings</h5>
          </li>
          <li className={this.activeClassWhenPageName("apiTokens")}>
            <a href={jsRoutes.controllers.APITokenController.listTokens(null, teamId).url}>Ellipsis API tokens</a>
          </li>
          <li className={this.activeClassWhenPageName("githubConfig")}>
            <a href={jsRoutes.controllers.GithubConfigController.index(teamId).url}>GitHub authentication</a>
          </li>
        </ul>
      </nav>
    );
  }
}

export default SettingsMenu;
