define(function(require) {
  var React = require('react'),
    RequiredAWSConfig = require('../models/required_aws_config'),
    RequiredOAuth2Application = require('../models/required_oauth2_application');

  return React.createClass({
    propTypes: {
      requiredAWSConfigs: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredAWSConfig)).isRequired,
      requiredOAuth2Applications: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredOAuth2Application)).isRequired,
      requiredSimpleTokenApis: React.PropTypes.arrayOf(React.PropTypes.object).isRequired, // TODO: use a class
      onApiConfigClick: React.PropTypes.func.isRequired,
      onAddApiConfigClick:  React.PropTypes.func.isRequired
    },

    onApiConfigClick: function(required) {
      this.props.onApiConfigClick(required);
    },

    renderConfig: function(required) {
      return (
        <div className="plxl mobile-pll">
          <a onClick={this.onApiConfigClick.bind(this, required)} className="link-block">
            {required.nameInCode}
          </a>
        </div>
      );
    },

    renderConfigs: function(configs) {
      return (
        configs.map((required, index) => (
          <div
            key={`apiConfig${index}`}
            className={`pvxs`}
          >
            {this.renderConfig(required)}
          </div>
        ))
      );
    },

    render: function() {
      return (
        <div className="border-bottom mtl pbl">
          <div className="container container-wide mbs">
            <h6>Api integrations</h6>
          </div>
          <div className="type-s">
            {this.renderConfigs(this.props.requiredAWSConfigs)}
            {this.renderConfigs(this.props.requiredOAuth2Applications)}
            {this.renderConfigs(this.props.requiredSimpleTokenApis)}
          </div>
          <div className="container container-wide mvm">
            <button type="button"
                    onClick={this.props.onAddConfiguration}
                    className="button button-s button-shrink">Add an API integration</button>
          </div>
        </div>
      );
    },
  });

});
