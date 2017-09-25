define(function(require) {
  var React = require('react'),
    aws = require('../models/aws'),
    AWSConfigRef = aws.AWSConfigRef,
    RequiredAWSConfig = aws.RequiredAWSConfig;

  return React.createClass({
    displayName: 'NotificationForMissingAWSConfig',
    propTypes: {
      details: React.PropTypes.arrayOf(React.PropTypes.shape({
        kind: React.PropTypes.string.isRequired,
        name: React.PropTypes.string.isRequired,
        existingAWSConfigs: React.PropTypes.arrayOf(React.PropTypes.instanceOf(AWSConfigRef)).isRequired,
        requiredAWSConfig: React.PropTypes.instanceOf(RequiredAWSConfig).isRequired,
        onUpdateAWSConfig: React.PropTypes.func.isRequired,
        onNewAWSConfig: React.PropTypes.func.isRequired,
        onConfigClick: React.PropTypes.func.isRequired
      })).isRequired
    },

    addAWSConfigPrompt: function(detail) {
      var matchingConfigs = detail.existingAWSConfigs;
      if (matchingConfigs.length === 1) {
        const config = matchingConfigs[0];
        return (
          <span className="mhxs">
            <button
              type="button"
              className="button-raw link button-s"
              onClick={this.onUpdateAWSConfig.bind(this, detail, config)}>

              Add {config.displayName} to this skill

            </button>
          </span>
        );
      } else if (matchingConfigs.length === 0) {
        return (
          <span className="mhxs">
            <button
              type="button"
              className="button-raw link button-s"
              onClick={this.onNewAWSConfig.bind(this, detail, detail.requiredAWSConfig)}>

              Configure the AWS API for this skill

            </button>
          </span>
        );
      } else {
        return (
          <span className="mhxs">
            <button
              type="button"
              className="button-raw link button-s"
              onClick={detail.onConfigClick.bind(this, detail.requiredAWSConfig)}>

              Choose a configuration for {detail.requiredAWSConfig.codePath()}

            </button>
          </span>
        );
      }
    },

    onUpdateAWSConfig: function(detail, cfg) {
      detail.onUpdateAWSConfig(detail.requiredAWSConfig.clone({
        config: cfg
      }));
    },

    onNewAWSConfig: function(detail, requiredAWSConfig) {
      detail.onNewAWSConfig(requiredAWSConfig);
    },

    render: function() {
      var detail = this.props.details[0];
      return (
        <span>
          <span>This skill needs to be configured to use the <b>AWS</b> API.</span>
          {this.addAWSConfigPrompt(detail)}
        </span>
      );
    }
  });
});
