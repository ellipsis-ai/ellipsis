define(function(require) {
  var React = require('react'),
    Collapsible = require('../collapsible'),
    HelpButton = require('../help/help_button'),
    HelpPanel = require('../help/panel'),
    SettingsMenu = require('../settings_menu');

  return React.createClass({
    displayName: 'ApplicationList',
    propTypes: {
      csrfToken: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired,
      apis: React.PropTypes.arrayOf(React.PropTypes.object),
      applications: React.PropTypes.arrayOf(React.PropTypes.object)
    },

    getInitialState: function() {
      return {
        activePanel: null
      };
    },

    hasApis: function() {
      return !!(this.props.apis && this.props.apis.length > 0);
    },

    getApplications: function() {
      return this.props.applications || [];
    },

    getGroupedApplications: function() {
      var flatApps = this.getSortedApplications();
      var groupedApps = {};
      flatApps.forEach(ea => {
        if (groupedApps[ea.apiId]) {
          groupedApps[ea.apiId].push(ea);
        } else {
          groupedApps[ea.apiId] = [ea];
        }
      });
      return groupedApps;
    },

    getSortedApplications: function() {
      return this.getApplications().sort((a, b) => {
        const aName = a.displayName.toLowerCase();
        const bName = b.displayName.toLowerCase();
        if (aName < bName) {
          return -1;
        } else if (aName > bName) {
          return 1;
        } else {
          return 0;
        }
      });
    },

    getApiNameForId: function(apiId) {
      var found = this.props.apis.find(ea => ea.apiId === apiId);
      return found ? found.name : "";
    },

    hasApplications: function() {
      return !!(this.props.applications && this.props.applications.length > 0);
    },

    toggleOAuth2ApplicationHelp: function() {
      this.setState({
        activePanel: this.state.activePanel === "oAuth2ApplicationHelp" ? null : "oAuth2ApplicationHelp"
      });
    },

    getActivePanel: function() {
      return this.state.activePanel;
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

                  <p>
                    <span>API applications allow you to use Ellipsis to access data from other products that </span>
                    <span>you use, using another product’s API (application programming interface). </span>
                  </p>

                  <p>
                    <span>Once a product is configured, each user can quickly grant permission for that </span>
                    <span>product to share their data with Ellipsis.</span>
                  </p>

                  <p>
                    <HelpButton className="mrs" onClick={this.toggleOAuth2ApplicationHelp}
                      toggled={this.getActivePanel() === 'oAuth2ApplicationHelp'}/>
                    <button type="button" className="button-raw" onClick={this.toggleOAuth2ApplicationHelp}>
                      How API applications work
                    </button>
                  </p>

                  <hr />

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
            <div className="flex flex-left"></div>
            <div className="flex flex-right bg-white"></div>
          </div>

          <footer ref="footer" className="position-fixed-bottom position-z-front border-top">
            <Collapsible ref="oAuth2ApplicationHelp" revealWhen={this.getActivePanel() === 'oAuth2ApplicationHelp'}>
              {this.renderOAuth2ApplicationHelp()}
            </Collapsible>
          </footer>
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
      var grouped = this.getGroupedApplications();
      var route = jsRoutes.controllers.OAuth2ApplicationController.edit;
      var groupKeys = Object.keys(grouped);
      return (
        <div>
          {this.renderApplicationCountDescription(this.getApplications().length, groupKeys.length)}

          {groupKeys.map((key, groupIndex) => {
            var group = grouped[key];
            var groupName = this.getApiNameForId(key);
            if (group.length === 1 && groupName.toLowerCase() === group[0].displayName.toLowerCase()) {
              return (
                <div key={`oAuthApplicationGroup${groupIndex}`} className="mvm">
                  <h4><a href={route(group[0].applicationId).url}>{groupName}</a></h4>
                </div>
              );
            } else {
              return (
                <div key={`oAuthApplicationGroup${groupIndex}`} className="mvm">
                  <h4 className="mbxs">{groupName}</h4>
                  <ul className="list-space-s">
                    {group.map((app, appIndex) => {
                      return (
                        <li key={`oAuthApplicationGroup${groupIndex}-${appIndex}`}>
                          <a href={route(app.applicationId).url}>{app.displayName}</a>
                        </li>
                      );
                    })}
                  </ul>
                </div>
              );
            }
          })}
        </div>
      );
    },

    renderNewApplicationLink: function() {
      return (
        <div className="mvxl">
          <a className="button"
            href={jsRoutes.controllers.OAuth2ApplicationController.newApp().url}
          >
            Add a new API application
          </a>
        </div>
      );
    },

    renderApplicationCountDescription: function(appCount, apiGroupCount) {
      if (appCount === 1) {
        return (
          <p>There is one API application configured.</p>
        );
      } else if (apiGroupCount === 1) {
        return (
          <p>There are {appCount} API applications configured.</p>
        );
      } else {
        return (
          <p>There are {appCount} API applications configured for {apiGroupCount} products.</p>
        );
      }
    },

    renderOAuth2ApplicationHelp: function() {
      return (
        <HelpPanel
          heading="Configuring an OAuth2 API application"
          onCollapseClick={this.toggleOAuth2ApplicationHelp}
        >
          <p>
            <span>In order to connect Ellipsis to other products securely, </span>
            <span>you need to tell the other product how to recognize Ellipsis, and tell Ellipsis how </span>
            <span>to authenticate with the other product’s API using OAuth2.</span>
          </p>

          <p>
            <span>An OAuth2 application configuration tells Ellipsis:</span>
          </p>

          <ul>
            <li>which product API to use,</li>
            <li>how to identify itself to the other product (using a client ID and secret), and</li>
            <li>the “scope” (level of access) to request.</li>
          </ul>

          <p>
            <span>Obtaining a client ID and secret requires you to register Ellipsis as a client application </span>
            <span>with the other product.</span>
          </p>

          <p>
            <span>Once configured, Ellipsis will be able to talk to the other product, while ensuring </span>
            <span>that each user authenticates separately (no credentials are shared). </span>
            <span>For better security, you should make separate configurations for each scope you deem </span>
            <span>appropriate so that, for example, only certain behaviors can make requests to modify data.</span>
          </p>
        </HelpPanel>
      );
    }
  });
});
