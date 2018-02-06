import * as React from 'react';

const SettingsMenu = React.createClass({
    propTypes: {
      teamId: React.PropTypes.string.isRequired,
      activePage: React.PropTypes.string,
      isAdmin: React.PropTypes.bool.isRequired
    },

    activeClassWhenPageName: function(pageName) {
      return this.props.activePage === pageName ? "list-nav-active-item" : "";
    },

    render: function () {
      return (
        <nav className="mvxxl">
          <ul className="list-nav">
            <li className={this.activeClassWhenPageName("regionalSettings")}>
              <a href={jsRoutes.controllers.web.settings.RegionalSettingsController.index(this.props.teamId).url}>Regional settings</a>
            </li>
            <li className={this.activeClassWhenPageName("environmentVariables")}>
              <a href={jsRoutes.controllers.web.settings.EnvironmentVariablesController.list(this.props.teamId).url}>Environment variables</a>
            </li>
            <li className={this.activeClassWhenPageName("apiTokens")}>
              <a href={jsRoutes.controllers.APITokenController.listTokens(null, this.props.teamId).url}>Ellipsis API tokens</a>
            </li>
            <li className={this.activeClassWhenPageName("oauthApplications")}>
              <a href={jsRoutes.controllers.web.settings.IntegrationsController.list(this.props.teamId).url}>Integrations</a>
            </li>
            <li className={this.activeClassWhenPageName("githubConfig")}>
              <a href={jsRoutes.controllers.GithubConfigController.index(this.props.teamId).url}>GitHub configuration</a>
            </li>
          </ul>
        </nav>
      );
    }
});

export default SettingsMenu;
