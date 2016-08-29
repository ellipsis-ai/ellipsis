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
      var route = jsRoutes.controllers.ApplicationController.editOAuth2Application;
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
                  <h4 className="mbn">{groupName}</h4>
                  <ul>
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
            href={jsRoutes.controllers.ApplicationController.newOAuth2Application().url}
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
    }
  });
});
