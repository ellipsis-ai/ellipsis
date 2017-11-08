define(function (require) {
  var React = require('react');

  return React.createClass({
    propTypes: {
      teamId: React.PropTypes.string.isRequired,
      activePage: React.PropTypes.string
    },

    activeClassWhenPageName: function(pageName) {
      return this.props.activePage === pageName ? "list-nav-active-item" : "";
    },

    render: function () {
      return (
        <nav className="mvxxl">
          <ul className="list-nav">
            <li className={this.activeClassWhenPageName("regionalSettings")}>
              <a href={jsRoutes.controllers.RegionalSettingsController.index(this.props.teamId).url}>Regional settings</a>
            </li>
            <li className={this.activeClassWhenPageName("environmentVariables")}>
              <a href={jsRoutes.controllers.EnvironmentVariablesController.list(this.props.teamId).url}>Environment variables</a>
            </li>
            <li className={this.activeClassWhenPageName("apiTokens")}>
              <a href={jsRoutes.controllers.APITokenController.listTokens(null, this.props.teamId).url}>Ellipsis API tokens</a>
            </li>
            <li className={this.activeClassWhenPageName("oauthApplications")}>
              <a href={jsRoutes.controllers.web.settings.IntegrationsController.list(this.props.teamId).url}>Integrations</a>
            </li>
          </ul>
        </nav>
      );p
    }
  });
});
