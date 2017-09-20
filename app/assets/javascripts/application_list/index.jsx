define(function(require) {
  var React = require('react'),
    Collapsible = require('../shared_ui/collapsible'),
    HelpButton = require('../help/help_button'),
    HelpPanel = require('../help/panel'),
    PageWithPanels = require('../shared_ui/page_with_panels'),
    SettingsMenu = require('../shared_ui/settings_menu'),
    Sort = require('../lib/sort');

  const ApplicationList = React.createClass({
    displayName: 'ApplicationList',
    propTypes: Object.assign(PageWithPanels.requiredPropTypes(), {
      csrfToken: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired,
      apis: React.PropTypes.arrayOf(React.PropTypes.object),
      applications: React.PropTypes.arrayOf(React.PropTypes.object)
    }),

    hasApis: function() {
      return !!(this.props.apis && this.props.apis.length > 0);
    },

    getApplications: function() {
      return this.props.applications || [];
    },

    getGroupedApplications: function() {
      var flatApps = Sort.arrayAlphabeticalBy(this.getApplications(), (item) => item.displayName);
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

    getApiNameForId: function(apiId) {
      var found = this.props.apis.find(ea => ea.apiId === apiId);
      return found ? found.name : "";
    },

    hasApplications: function() {
      return !!(this.props.applications && this.props.applications.length > 0);
    },

    toggleOAuth2ApplicationHelp: function() {
      this.props.onToggleActivePanel("oAuth2ApplicationHelp");
    },

    render: function() {
      return (
        <div>
          <div className="bg-light">
            <div className="container container-wide pbm">
              {this.renderHeader()}
            </div>
          </div>
          <div className="flex-columns">
            <div className="flex-column flex-column-left container container-wide prn">
              <div className="columns">
                <div className="column column-one-quarter">
                  <SettingsMenu activePage="oauthApplications" teamId={this.props.teamId} />
                </div>
                <div className="column column-three-quarters bg-white border-radius-bottom-left ptxl pbxxxxl phxxxxl">

                  <p>
                    <span>API configurations allow you to use Ellipsis to access data from other products that </span>
                    <span>you use, using another product’s API (application programming interface). </span>
                  </p>

                  <p>
                    <span>Once a product is configured, each user can quickly grant permission for that </span>
                    <span>product to share their data with Ellipsis.</span>
                  </p>

                  <p>
                    <HelpButton className="mrs" onClick={this.toggleOAuth2ApplicationHelp}
                      toggled={this.props.activePanelName === 'oAuth2ApplicationHelp'}/>
                    <button type="button" className="button-raw" onClick={this.toggleOAuth2ApplicationHelp}>
                      How API configurations work
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
            <div className="flex-column flex-column-right bg-white"></div>
          </div>

          <footer ref="footer" className="position-fixed-bottom position-z-front border-top">
            <Collapsible ref="oAuth2ApplicationHelp" revealWhen={this.props.activePanelName === 'oAuth2ApplicationHelp'}>
              {this.renderOAuth2ApplicationHelp()}
            </Collapsible>
          </footer>
        </div>
      );
    },

    renderHeader: function() {
      return (
        <h3 className="mvn ptxxl type-weak display-ellipsis">
          <span className="mrs">API configurations</span>
        </h3>
      );
    },

    renderNoApplications: function() {
      return (
        <div>
          <p><b>No APIs have been configured.</b></p>

          <p>
            Each configuration specifies:
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
                  <h4><a href={route(group[0].id).url}>{groupName}</a></h4>
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
                          <a href={route(app.id, this.props.teamId).url}>{app.displayName}</a>
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
            href={jsRoutes.controllers.OAuth2ApplicationController.newApp(null, null, this.props.teamId, null).url}
          >
            Add a new API configuration
          </a>
        </div>
      );
    },

    renderApplicationCountDescription: function(appCount, apiGroupCount) {
      if (appCount === 1) {
        return (
          <p>There is one API configuration.</p>
        );
      } else if (apiGroupCount === 1) {
        return (
          <p>There are {appCount} API configurations.</p>
        );
      } else {
        return (
          <p>There are {appCount} API configurations for {apiGroupCount} products.</p>
        );
      }
    },

    renderOAuth2ApplicationHelp: function() {
      return (
        <HelpPanel
          heading="Configuring an OAuth2 API configuration"
          onCollapseClick={this.toggleOAuth2ApplicationHelp}
        >
          <p>
            <span>In order to connect Ellipsis to other products securely, </span>
            <span>you need to tell the other product how to recognize Ellipsis, and tell Ellipsis how </span>
            <span>to authenticate with the other product’s API using OAuth2.</span>
          </p>

          <p>
            <span>An OAuth2 API configuration tells Ellipsis:</span>
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
            <span>appropriate so that, for example, only certain skills can make requests to modify data.</span>
          </p>
        </HelpPanel>
      );
    }
  });

  return PageWithPanels.with(ApplicationList);
});
