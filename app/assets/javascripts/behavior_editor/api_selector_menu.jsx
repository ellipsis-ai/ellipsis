define(function(require) {
  var React = require('react'),
    BehaviorConfig = require('../models/behavior_config'),
    DropdownMenu = require('../shared_ui/dropdown_menu');

  return React.createClass({
    displayName: "APISelectorMenu",
    propTypes: {
      openWhen: React.PropTypes.bool.isRequired,
      behaviorConfig: React.PropTypes.instanceOf(BehaviorConfig),
      toggle: React.PropTypes.func.isRequired,
      allAWSConfigs: React.PropTypes.arrayOf(React.PropTypes.shape({
        configId: React.PropTypes.string.isRequired,
        displayName: React.PropTypes.string.isRequired
      })),
      requiredAWSConfigs: React.PropTypes.arrayOf(React.PropTypes.shape({
        id: React.PropTypes.string.isRequired,
        config: React.PropTypes.shape({
          configId: React.PropTypes.string.isRequired,
          displayName: React.PropTypes.string.isRequired
        })
      })),
      allOAuth2Applications: React.PropTypes.arrayOf(React.PropTypes.shape({
        applicationId: React.PropTypes.string.isRequired,
        displayName: React.PropTypes.string.isRequired
      })).isRequired,
      requiredOAuth2ApiConfigs: React.PropTypes.arrayOf(React.PropTypes.shape({
        apiId: React.PropTypes.string.isRequired,
        recommendedScope: React.PropTypes.string,
        application: React.PropTypes.shape({
          applicationId: React.PropTypes.string.isRequired,
          displayName: React.PropTypes.string.isRequired
        })
      })).isRequired,
      allSimpleTokenApis: React.PropTypes.arrayOf(React.PropTypes.shape({
        apiId: React.PropTypes.string.isRequired,
        name: React.PropTypes.string.isRequired
      })).isRequired,
      requiredSimpleTokenApis: React.PropTypes.arrayOf(React.PropTypes.shape({
        apiId: React.PropTypes.string.isRequired,
        name: React.PropTypes.string.isRequired
      })).isRequired,
      onAddAWSConfig: React.PropTypes.func.isRequired,
      onRemoveAWSConfig: React.PropTypes.func.isRequired,
      onAddOAuth2Application: React.PropTypes.func.isRequired,
      onRemoveOAuth2Application: React.PropTypes.func.isRequired,
      onAddSimpleTokenApi: React.PropTypes.func.isRequired,
      onRemoveSimpleTokenApi: React.PropTypes.func.isRequired,
      onNewOAuth2Application: React.PropTypes.func.isRequired,
      getOAuth2ApiWithId: React.PropTypes.func.isRequired
    },

    getAPISelectorDropdownLabel: function() {
      var activeAWSConfigs = this.props.requiredAWSConfigs.filter(ea => !!ea.config);
      var activeApiConfigs = this.props.requiredOAuth2ApiConfigs.filter((ea) => !!ea.application);
      var activeAPICount = activeAWSConfigs.length + activeApiConfigs.length + this.props.requiredSimpleTokenApis.length;
      if (activeAPICount > 0) {
        return (
          <span>
          <span>Third-party APIs in skill </span>
          <span className="type-semibold">({activeAPICount} active)</span>
        </span>
        );
      } else {
        return "Add third-party APIs to this skill";
      }
    },

    getAPISelectorLabelForApi: function(api, displayName) {
      if (api && api.iconImageUrl) {
        return (
          <div className="columns columns-elastic">
            <div className="column column-shrink prs align-m">
              <img src={api.iconImageUrl} width="24" height="24"/>
            </div>
            <div className="column column-expand align-m">
              {displayName}
            </div>
          </div>
        );
      } else if (api && api.logoImageUrl) {
        return (
          <div className="columns columns-elastic">
            <div className="column column-shrink prs align-m">
              <img src={api.logoImageUrl} height="18" />
            </div>
            <div className="column column-expand align-m">
              {displayName}
            </div>
          </div>
        );
      } else {
        return (
          <span>{displayName}</span>
        );
      }
    },

    getAWSSelectorLabelForConfig: function(cfg) {
      return (
        <div className="columns columns-elastic">
          <div className="column column-shrink prs align-m">
            <img src="/assets/images/logos/aws_logo_web_300px.png" height="32"/>
          </div>
          <div className="column column-expand align-m">
            {cfg.displayName}
          </div>
        </div>
      );
    },

    getAPISelectorLabelForApp: function(app) {
      var api = this.props.getOAuth2ApiWithId(app.apiId);
      return this.getAPISelectorLabelForApi(api, app.displayName);
    },

    isRequiredAWSConfig: function(cfg) {
      var index = this.props.requiredAWSConfigs.findIndex(function(ea) {
        return ea.config && ea.config.configId === cfg.configId;
      });
      return index >= 0;
    },

    isRequiredOAuth2Application: function(app) {
      var appIndex = this.props.requiredOAuth2ApiConfigs.findIndex(function(ea) {
        return ea.application && ea.application.applicationId === app.applicationId;
      });
      return appIndex >= 0;
    },

    toggleAWSConfig: function(cfg) {
      if (this.isRequiredAWSConfig(cfg)) {
        this.props.onRemoveAWSConfig(cfg);
      } else {
        this.props.onAddAWSConfig(cfg);
      }
    },

    toggleOAuth2Application: function(app) {
      if (this.isRequiredOAuth2Application(app)) {
        this.props.onRemoveOAuth2Application(app);
      } else {
        this.props.onAddOAuth2Application(app);
      }
    },

    isRequiredSimpleTokenApi: function(api) {
      var appIndex = this.props.requiredSimpleTokenApis.findIndex(function(ea) {
        return ea.apiId === api.apiId;
      });
      return appIndex >= 0;
    },

    toggleSimpleTokenApi: function(api) {
      if (this.isRequiredSimpleTokenApi(api)) {
        this.props.onRemoveSimpleTokenApi(api);
      } else {
        this.props.onAddSimpleTokenApi(api);
      }
    },

    addNewOAuth2Application: function() {
      this.props.onNewOAuth2Application();
    },

    render: function() {
      return (
        <DropdownMenu
          openWhen={this.props.openWhen}
          label={this.getAPISelectorDropdownLabel()}
          labelClassName="button-s"
          menuClassName="popup-dropdown-menu-wide popup-dropdown-menu-right mobile-popup-dropdown-menu-left"
          toggle={this.props.toggle}
        >
          {this.props.allAWSConfigs.map((cfg, index) => {
            return (
              <DropdownMenu.Item
                key={"aws-config-" + index}
                checkedWhen={this.isRequiredAWSConfig(cfg)}
                onClick={this.toggleAWSConfig.bind(this, cfg)}
                label={this.getAWSSelectorLabelForConfig(cfg)}
              />
            );
          })}
          {this.props.allOAuth2Applications.map((app, index) => {
            return (
              <DropdownMenu.Item
                key={"oauth2-app-" + index}
                checkedWhen={this.isRequiredOAuth2Application(app)}
                onClick={this.toggleOAuth2Application.bind(this, app)}
                label={this.getAPISelectorLabelForApp(app)}
              />
            );
          })}
          {this.props.allSimpleTokenApis.map((api, index) => {
            return (
              <DropdownMenu.Item
                key={"simple-token-api-" + index}
                checkedWhen={this.isRequiredSimpleTokenApi(api)}
                onClick={this.toggleSimpleTokenApi.bind(this, api)}
                label={this.getAPISelectorLabelForApi(api, api.name)}
              />
            );
          })}
          <DropdownMenu.Item
            onClick={this.addNewOAuth2Application}
            className="border-top"
            label="Add new API application…"
          />
        </DropdownMenu>
      );
    }
  });
});
