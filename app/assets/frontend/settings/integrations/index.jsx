import * as React from 'react';
import Collapsible from '../../shared_ui/collapsible';
import HelpButton from '../../help/help_button';
import HelpPanel from '../../help/panel';
import Page from '../../shared_ui/page';
import SettingsPage from '../../shared_ui/settings_page';
import Sort from '../../lib/sort';

const IntegrationList = React.createClass({
    propTypes: Object.assign({}, Page.requiredPropTypes, {
      csrfToken: React.PropTypes.string.isRequired,
      isAdmin: React.PropTypes.bool.isRequired,
      teamId: React.PropTypes.string.isRequired,
      oauthApis: React.PropTypes.arrayOf(React.PropTypes.object),
      oauthApplications: React.PropTypes.arrayOf(React.PropTypes.object),
      awsConfigs: React.PropTypes.arrayOf(React.PropTypes.object)
    }),

    getDefaultProps: function() {
      return Page.requiredPropDefaults();
    },

    hasApis: function() {
      return Boolean(this.getAllApis().length > 0);
    },

    getOAuthApis: function() {
      return this.props.oauthApis || [];
    },

    getAllApis: function() {
      return this.getOAuthApis();
    },

    getOAuthApplications: function() {
      return this.props.oauthApplications || [];
    },

    getAllApplications: function() {
      return this.getOAuthApplications();
    },

    getGroupedApplications: function() {
      const flatApps = Sort.arrayAlphabeticalBy(this.getAllApplications(), (item) => item.displayName);
      const groupedApps = {};
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
      const found = this.getAllApis().find(ea => ea.apiId === apiId);
      return found ? found.name : "";
    },

    hasApplications: function() {
      return Boolean(this.getAllApplications().length > 0);
    },

    hasAwsConfigs: function() {
      return Boolean(this.props.awsConfigs && this.props.awsConfigs.length > 0);
    },

    getAwsConfigs: function() {
      return this.props.awsConfigs || [];
    },

    toggleOAuthApplicationHelp: function() {
      this.props.onToggleActivePanel("oAuthApplicationHelp");
    },

    render: function() {
      return (
        <SettingsPage teamId={this.props.teamId} isAdmin={this.props.isAdmin} activePage={"oauthApplications"}>

          <p>
            <span>Create a new configuration to give Ellipsis access to third-party APIs, </span>
            <span>services, and data.</span>
          </p>

          <p>
            <HelpButton className="mrs" onClick={this.toggleOAuthApplicationHelp}
                        toggled={this.props.activePanelName === 'oAuthApplicationHelp'}/>
            <button type="button" className="button-raw" onClick={this.toggleOAuthApplicationHelp}>
              How Integrations work
            </button>
          </p>

          <hr />

          <Collapsible revealWhen={this.hasAwsConfigs()}>
            {this.renderAwsConfigs()}
          </Collapsible>

          <Collapsible revealWhen={!this.hasAwsConfigs()}>
            {this.renderNoAwsConfigs()}
          </Collapsible>

          <Collapsible revealWhen={this.hasApplications()}>
            {this.renderApplicationList()}
          </Collapsible>

          <Collapsible revealWhen={!this.hasApplications()}>
            {this.renderNoApplications()}
          </Collapsible>

          <Collapsible revealWhen={this.hasApis()}>
            {this.renderNewIntegrationLink()}
          </Collapsible>

          {this.props.onRenderFooter((
            <Collapsible revealWhen={this.props.activePanelName === 'oAuthApplicationHelp'}>
              {this.renderOAuthApplicationHelp()}
            </Collapsible>
          ))}
        </SettingsPage>
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
            <li>the OAuth credentials (key and secret),</li>
            <li>and the scope (level of access) to use for requests.</li>
          </ul>
        </div>
      );
    },

    optionalTeamId: function() {
      return this.props.isAdmin ? this.props.teamId : null;
    },

    renderApplicationList: function() {
      var grouped = this.getGroupedApplications();
      var route = jsRoutes.controllers.web.settings.IntegrationsController.edit;
      var groupKeys = Object.keys(grouped);
      return (
        <div>

          {groupKeys.map((key, groupIndex) => {
            var group = grouped[key];
            var groupName = this.getApiNameForId(key);
            if (group.length === 1 && groupName.toLowerCase() === group[0].displayName.toLowerCase()) {
              return (
                <div key={`oAuthApplicationGroup${groupIndex}`} className="mvm">
                  <h4><a href={route(group[0].id, this.optionalTeamId()).url}>{groupName}</a></h4>
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
                          <a href={route(app.id, this.optionalTeamId()).url}>{app.displayName}</a>
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

    renderAwsConfigs: function() {
      var awsConfigs = this.getAwsConfigs();
      var route = jsRoutes.controllers.web.settings.AWSConfigController.edit;
      return (
        <div className="mvm">
          <h4>AWS</h4>
          <ul className="list-space-s">
            {awsConfigs.map((config, index) => {
              return (
                <li key={`awsConfig${index}`} className="mvm">
                  <a href={route(config.id, this.optionalTeamId()).url}>{config.displayName}</a>
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
             href={jsRoutes.controllers.web.settings.IntegrationsController.add(this.optionalTeamId(), null, null, null).url}
          >
            Add a new integration
          </a>
        </div>
      );
    },

    renderOAuthApplicationHelp: function() {
      return (
        <HelpPanel
          heading="Creating a new configuration"
          onCollapseClick={this.toggleOAuthApplicationHelp}
        >
          <p>
            <span>In order to connect Ellipsis to other products securely, </span>
            <span>you need to tell the other product how to recognize Ellipsis, and tell Ellipsis how </span>
            <span>to authenticate with the other product’s API using OAuth.</span>
          </p>

          <p>
            <span>An OAuth API configuration tells Ellipsis:</span>
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

export default IntegrationList;
