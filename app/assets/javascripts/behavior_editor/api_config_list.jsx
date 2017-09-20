define(function(require) {
  var React = require('react'),
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
      return (
        <div className="border-bottom mtl pbl">
          <div className="container container-wide mbs">
            <h6>API integrations</h6>
          </div>
          <div className="type-s">
            {this.renderConfigs(this.props.requiredAWSConfigs)}
            {this.renderConfigs(this.props.requiredOAuth2Applications)}
            {this.renderConfigs(this.props.requiredSimpleTokenApis)}
          </div>
          <div className="container container-wide mvm">
            <Button onClick={this.props.onAddApiConfigClick}
                    className="button button-s button-shrink">Add new integration</Button>
          </div>
        </div>
      );
    }

  });

});
