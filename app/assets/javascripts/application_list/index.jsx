define(function(require) {
  var React = require('react'),
    Collapsible = require('../collapsible'),
    SettingsMenu = require('../settings_menu');

  return React.createClass({
    displayName: 'ApplicationList',
    propTypes: {
      csrfToken: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired,
      apis: React.PropTypes.arrayOf(React.PropTypes.object),
      applications: React.PropTypes.arrayOf(React.PropTypes.object)
    },

    hasApis: function() {
      return !!(this.props.apis && this.props.apis.length > 0);
    },

    getApplications: function() {
      return this.props.applications || [];
    },

    hasApplications: function() {
      return !!(this.props.applications && this.props.applications.length > 0);
    },

    render: function() {
      return (
        <div>
          <div className="bg-light">
            <div className="container pbm">
              {this.renderHeader()}
            </div>
          </div>
          <div className="flex-container">
            <div className="container flex flex-center">
              <div className="columns">
                <div className="column column-one-quarter">
                  <SettingsMenu activePage="oauthApplications"/>
                </div>
                <div className="column column-three-quarters bg-white border-radius-bottom-left ptxl pbxxxxl phxxxxl">
                  <Collapsible revealWhen={this.hasApplications()}>
                    {this.renderApplicationList()}
                  </Collapsible>

                  <Collapsible revealWhen={!this.hasApplications()}>
                    {this.renderNoApplications()}
                  </Collapsible>

                  <Collapsible revealWhen={this.hasApis()}>
                    {this.renderNewApplicationLink()}
                  </Collapsible>
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    },

    renderHeader: function() {
      return (
        <h3 className="mvn ptxxl type-weak display-ellipsis">
          <span className="mrs">API applications</span>
        </h3>
      );
    },

    renderNoApplications: function() {
      return (
        <div>
          <p><b>There are no API applications configured.</b></p>

          <p>
            <span>API applications allow Ellipsis to access data from other products that </span>
            <span>you use, using an API (application programming interface). </span>
          </p>

          <p>
            Each application specifies:
          </p>

          <ul className="list-space-s">
            <li>which product API to use,</li>
            <li>the OAuth2 credentials (client key and secret),</li>
            <li>and the scope (level of access) to use for requests.</li>
          </ul>
        </div>
      );
    },

    renderApplicationList: function() {
      return this.getApplications().map(app => app.name).join(', ');
    },

    renderNewApplicationLink: function() {
      return (
        <div className="mvxl">
          <a className="button"
            href={jsRoutes.controllers.ApplicationController.newOAuth2Application().url}
          >
            Add a new API application
          </a>
        </div>
      );
    }
  });
});
