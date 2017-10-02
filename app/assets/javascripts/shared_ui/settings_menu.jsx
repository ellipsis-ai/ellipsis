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
            <li className={this.activeClassWhenPageName("apiTokens")}>
              <a href={jsRoutes.controllers.APITokenController.listTokens(null, this.props.teamId).url}>Tokens</a>
            </li>
            <li className={this.activeClassWhenPageName("oauthApplications")}>
              <a href={jsRoutes.controllers.OAuth2ApplicationController.list(this.props.teamId).url}>Integrations</a>
            </li>
            <li className={this.activeClassWhenPageName("environmentVariables")}>
              <a href={jsRoutes.controllers.EnvironmentVariablesController.list(this.props.teamId).url}>Environment Variables</a>
            </li>
            <li className={this.activeClassWhenPageName("awsConfigs")}>
            <a href={jsRoutes.controllers.AWSConfigController.list(this.props.teamId).url}>Amazon Web Services (AWS) configurations</a>
            </li>
          </ul>
        </nav>
      );
    }
  });
});
