define(function(require) {
  var React = require('react'),
    Collapsible = require('../shared_ui/collapsible'),
    HelpButton = require('../help/help_button'),
    HelpPanel = require('../help/panel'),
    PageWithPanels = require('../shared_ui/page_with_panels'),
    SettingsMenu = require('../shared_ui/settings_menu');

  const ConfigList = React.createClass({
    displayName: 'ConfigList',
    propTypes: Object.assign(PageWithPanels.requiredPropTypes(), {
      csrfToken: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired,
      configs: React.PropTypes.arrayOf(React.PropTypes.object)
    }),

    getConfigs: function() {
      return this.props.configs || [];
    },

    hasConfigs: function() {
      return !!(this.getConfigs() && this.getConfigs().length > 0);
    },

    toggleHelp: function() {
      this.props.onToggleActivePanel("awsConfigHelp");
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
                  <SettingsMenu activePage="awsConfigs" teamId={this.props.teamId} />
                </div>
                <div className="column column-three-quarters bg-white border-radius-bottom-left ptxl pbxxxxl phxxxxl">

                  <p>
                    <span>AWS configurations allow you to access your Amazon Web Services resources for a given profile.</span>
                  </p>

                  <p>
                    <HelpButton className="mrs" onClick={this.toggleHelp}
                                toggled={this.props.activePanelName === 'awsConfigHelp'}/>
                    <button type="button" className="button-raw" onClick={this.toggleHelp}>
                      How AWS configurations work
                    </button>
                  </p>

                  <hr />

                  <Collapsible revealWhen={this.hasConfigs()}>
                    {this.renderConfigList()}
                  </Collapsible>

                  <Collapsible revealWhen={!this.hasConfigs()}>
                    {this.renderNoConfigs()}
                  </Collapsible>

                  {this.renderNewConfigLink()}
                </div>
              </div>
            </div>
            <div className="flex-column flex-column-right bg-white"></div>
          </div>

          <footer ref="footer" className="position-fixed-bottom position-z-front border-top">
            <Collapsible ref="awsConfigHelp" revealWhen={this.props.activePanelName === 'awsConfigHelp'}>
              {this.renderHelp()}
            </Collapsible>
          </footer>
        </div>
      );
    },

    renderHeader: function() {
      return (
        <h3 className="mvn ptxxl type-weak display-ellipsis">
          <span className="mrs">Amazon Web Services (AWS) configurations</span>
        </h3>
      );
    },

    renderNoConfigs: function() {
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

    renderConfigList: function() {
      var configs = this.getConfigs();
      var route = jsRoutes.controllers.AWSConfigController.edit;
      return (
        <div>
          {this.renderConfigCountDescription(this.getConfigs().length)}

          {configs.map((config, index) => {
            return (
              <div key={`awsConfig${index}`} className="mvm">
                <h4><a href={route(config.configId).url}>{config.name}</a></h4>
              </div>
            );
          })}
        </div>
      );
    },

    renderNewConfigLink: function() {
      return (
        <div className="mvxl">
          <a className="button"
             href={jsRoutes.controllers.AWSConfigController.newConfig(this.props.teamId, null).url}
          >
            Add a new AWS configuration
          </a>
        </div>
      );
    },

    renderConfigCountDescription: function(configCount) {
      if (configCount === 1) {
        return (
          <p>There is one configuration.</p>
        );
      } else {
        return (
          <p>There are {configCount} configurations.</p>
        );
      }
    },

    renderHelp: function() {
      return (
        <HelpPanel
          heading="Configuring AWS"
          onCollapseClick={this.toggleHelp}
        >
          <p>
            <span>In order to connect Ellipsis to an Amazon Web Services (AWS) profile, </span>
            <span>you need to provide the appropriate credentials</span>
          </p>

          <p>
            <span>Once configured, Ellipsis will be able to access resources using the AWS API. </span>
            <span>For better security, you should make separate configurations for each type of access you deem </span>
            <span>appropriate so that, for example, only certain skills can make requests to modify data.</span>
          </p>
        </HelpPanel>
      );
    }
  });

  return PageWithPanels.with(ConfigList);
});
