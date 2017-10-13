define(function(require) {
  var React = require('react'),
    Collapsible = require('../../shared_ui/collapsible'),
    HelpButton = require('../../help/help_button'),
    HelpPanel = require('../../help/panel'),
    Page = require('../../shared_ui/page'),
    SettingsMenu = require('../../shared_ui/settings_menu'),
    Sort = require('../../lib/sort');

  const ApplicationList = React.createClass({
    propTypes: Object.assign({}, Page.requiredPropTypes, {
      csrfToken: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired,
      apis: React.PropTypes.arrayOf(React.PropTypes.object),
      applications: React.PropTypes.arrayOf(React.PropTypes.object),
      awsConfigs: React.PropTypes.arrayOf(React.PropTypes.object)
    }),

    getDefaultProps: function() {
      return Page.requiredPropDefaults();
    },

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

    hasAWSConfigs: function() {
      return !!(this.props.awsConfigs && this.props.awsConfigs.length > 0);
    },

    getAWSConfigs: function() {
      return this.props.awsConfigs || [];
    },

    toggleOAuth2ApplicationHelp: function() {
      this.props.onToggleActivePanel("oAuth2ApplicationHelp");
    },

    render: function() {
      return (
        <div className="flex-row-cascade">
          <div className="bg-light">
            <div className="container container-wide pbm">
              {this.renderHeader()}
            </div>
          </div>
          <div className="flex-columns flex-row-expand">
            <div className="flex-column flex-column-left flex-rows container container-wide prn">
              <div className="columns flex-columns flex-row-expand">
                <div className="column column-one-quarter flex-column">
                  <SettingsMenu activePage="oauthApplications" teamId={this.props.teamId} />
                </div>
                <div className="column column-three-quarters flex-column bg-white ptxl pbxxxxl phxxxxl">

                  <p>
                    <span>Create a new configuration to give Ellipsis access to third-party APIs, </span>
                    <span>services, and data.</span>
                  </p>

                  <p>
                    <HelpButton className="mrs" onClick={this.toggleOAuth2ApplicationHelp}
                                toggled={this.props.activePanelName === 'oAuth2ApplicationHelp'}/>
                    <button type="button" className="button-raw" onClick={this.toggleOAuth2ApplicationHelp}>
                      How Integrations work
                    </button>
                  </p>

                  <hr />

                  <Collapsible revealWhen={this.hasAWSConfigs()}>
                    {this.renderAWSConfigs()}
                  </Collapsible>

                  <Collapsible revealWhen={!this.hasAWSConfigs()}>
                    {this.renderNoAwsConfigs()}
                  </Collapsible>

                  <Collapsible revealWhen={this.hasApplications()}>
                    {this.renderApplicationList()}
                  </Collapsible>

                  <Collapsible revealWhen={!this.hasApplications()}>
                    {this.renderNoApplications()}
                  </Collapsible>

                  <Collapsible revealWhen={this.hasApis()}>
                    {this.renderNewIntegrationLink2()}
                  </Collapsible>

                </div>
              </div>
            </div>
            <div className="flex-column flex-column-right bg-white" />
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

    renderNoAwsConfigs: function() {
      return (
        <div>
          <p><b>There are no AWS configurations.</b></p>

          <p>
            Each configuration specifies:
          </p>

          <ul className="list-space-s">
            <li>the access key for the AWS profile</li>
            <li>the secret key</li>
            <li>and the region</li>
          </ul>
        </div>
      );
    },

    renderAWSConfigs: function() {
      var awsConfigs = this.getAWSConfigs();
      var route = jsRoutes.controllers.AWSConfigController.edit;
      return (
        <div className="mvm">
          <h4>AWS</h4>
          <ul className="list-space-s">
            {awsConfigs.map((config, index) => {
              return (
                <li key={`awsConfig${index}`} className="mvm">
                  <a href={route(config.id).url}>{config.displayName}</a>
                </li>
              );
            })}
          </ul>
        </div>
      );
    },

    renderNewIntegrationLink: function() {
      return (
        <div className="mvxl">
          <a className="button"
             href={jsRoutes.controllers.web.settings.IntegrationsController.add(this.props.teamId, null, null, null).url}
          >
            Add a new integration
          </a>
        </div>
      );
    },

    renderNewIntegrationLink2: function() {
      return (
        <div className="mvxl">
          <a className="button"
             href={jsRoutes.controllers.web.settings.IntegrationsController.add(this.props.teamId, null, null, null).url}
          >
            Add a new integration
          </a>
        </div>
      );
    },

    renderApplicationCountDescription: function(appCount, apiGroupCount) {
      if (appCount === 1) {
        return (
          <p>There is one configuration.</p>
        );
      } else if (apiGroupCount === 1) {
        return (
          <p>There are {appCount} configurations.</p>
        );
      } else {
        return (
          <p>There are {appCount} configurations for {apiGroupCount} services.</p>
        );
      }
    },

    renderOAuth2ApplicationHelp: function() {
      return (
        <HelpPanel
          heading="Creating a new configuration"
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

  return ApplicationList;
});
