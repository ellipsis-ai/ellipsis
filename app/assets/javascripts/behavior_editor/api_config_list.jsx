define(function(require) {
  var React = require('react'),
    AddButton = require('../form/add_button'),
    Button = require('../form/button'),
    RequiredAWSConfig = require('../models/aws').RequiredAWSConfig,
    RequiredOAuth2Application = require('../models/oauth2').RequiredOAuth2Application,
    RequiredSimpleTokenApi = require('../models/simple_token').RequiredSimpleTokenApi;

  return React.createClass({
    propTypes: {
      requiredAWSConfigs: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredAWSConfig)).isRequired,
      requiredOAuth2Applications: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredOAuth2Application)).isRequired,
      requiredSimpleTokenApis: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredSimpleTokenApi)).isRequired,
      onApiConfigClick: React.PropTypes.func.isRequired,
      onAddApiConfigClick:  React.PropTypes.func.isRequired,
      getApiConfigName: React.PropTypes.func.isRequired
    },

    onApiConfigClick: function(required) {
      this.props.onApiConfigClick(required);
    },

    renderConfig: function(required) {
      const name = this.props.getApiConfigName(required);
      const path = required.codePath();
      const onClick = this.onApiConfigClick.bind(this, required);
      return (
        <div className="plxl mobile-pll">
          <Button onClick={onClick} className="button-block">
            <span className="link">{name}</span>
            <span className="type-pink type-bold type-italic">
              {required.isConfigured() ? null : " — Unconfigured"}
            </span>
          </Button>
          <div className="display-limit-width display-overflow-hidden" title={path}>
            <code className="type-weak">{path}</code>
          </div>
        </div>
      );
    },

    renderConfigs: function(configs) {
      return configs.map((required, index) => (
        <div key={`apiConfig${index}`} className={`pvxs`}>
          {this.renderConfig(required)}
        </div>
      ));
    },

    render: function() {
      const awsConfigs = this.renderConfigs(this.props.requiredAWSConfigs);
      const oAuth2Configs = this.renderConfigs(this.props.requiredOAuth2Applications);
      const simpleTokenConfigs = this.renderConfigs(this.props.requiredSimpleTokenApis);
      const hasConfigs = awsConfigs.length > 0 || oAuth2Configs.length > 0 || simpleTokenConfigs.length > 0;
      return (
        <div className="border-bottom pbl">
          <div className="container container-wide prl">
            <div className="columns columns-elastic">
              <div className="column column-expand ptl">
                <h6>API integrations</h6>
              </div>
              <div className="column column-shrink ptm type-link">
                <AddButton
                  onClick={this.props.onAddApiConfigClick}
                  label={"Add new integration"}
                />
              </div>
            </div>
          </div>
          <div className={`type-s ${hasConfigs ? "mts" : ""}`}>
            {awsConfigs}
            {oAuth2Configs}
            {simpleTokenConfigs}
          </div>
        </div>
      );
    }

  });

});
