define(function(require) {
  var React = require('react'),
    Collapsible = require('./collapsible'),
    SVGTip = require('./svg/tip'),
    SVGWarning = require('./svg/warning');

  return React.createClass({
    propTypes: {
      details: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      kind: React.PropTypes.string.isRequired,
      index: React.PropTypes.number.isRequired,
      onClick: React.PropTypes.func.isRequired,
      hidden: React.PropTypes.bool
    },

    getNotificationForKind: function(kind) {
      if (kind === "env_var_not_defined") {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: this.getNotificationForEnvVarMissing()
        };
      } else if (kind === "oauth2_config_without_application") {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: this.getNotificationForMissingOAuth2Application()
        };
      } else if (kind === "oauth2_application_unused") {
        return {
          containerClass: "box-tip",
          icon: this.getTipIcon(),
          message: this.getNotificationForUnusedOAuth2Application()
        };
      } else if (kind === "aws_unused") {
        return {
          containerClass: "box-tip",
          icon: this.getTipIcon(),
          message: this.getNotificationForUnusedAWS()
        };
      }
    },

    getWarningIcon: function() {
      return (
        <span className="display-inline-block mrs align-b type-yellow" style={{ width: 22, height: 24 }}>
          <SVGWarning />
        </span>
      );
    },

    getTipIcon: function() {
      return (
        <span className="display-inline-block mrs align-b type-pink" style={{ width: 22, height: 24 }}>
          <SVGTip />
        </span>
      );
    },

    getNotificationForEnvVarMissing: function() {
      var numVarsMissing = this.props.details.length;
      if (numVarsMissing === 1) {
        var firstDetail = this.props.details[0];
        return (
          <span>
            <span>This behavior requires an environment variable named </span>
            {this.getButtonForEnvVar(firstDetail)}
            <span className="mlxs"> to work properly.</span>
          </span>
        );
      } else {
        return (
          <span>
            <span>This behavior requires the following environment variables to work properly: </span>
            {this.props.details.map(function(detail, index) {
              return (
                <span key={"notificationDetail" + index}>
                  {this.getButtonForEnvVar(detail)}
                  <span>{index + 1 < numVarsMissing ? ", " : ""}</span>
                </span>
              );
            }, this)}
          </span>
        );
      }
    },

    getButtonForEnvVar: function(envVarDetail) {
      return (
        <button
          type="button"
          className="button-raw button-s type-monospace type-bold mlxs"
          onClick={this.onClick.bind(this, envVarDetail)}
        >{envVarDetail.environmentVariableName}</button>
      );
    },

    onAddOAuth2Application: function(detail, app) {
      detail.onAddOAuth2Application(app);
    },

    onNewOAuth2Application: function(detail, requiredOAuth2ApiConfigId) {
      detail.onNewOAuth2Application(requiredOAuth2ApiConfigId);
    },

    addOAuth2ApplicationPrompt: function(detail) {
      var matchingApplication = detail.existingOAuth2Applications.find(ea => ea.apiId === detail.requiredApiConfig.apiId);
      if (matchingApplication) {
        return (
          <span className="mhxs">
            <button
              type="button"
              className="button-raw link button-s"
              onClick={this.onAddOAuth2Application.bind(this, detail, matchingApplication)}>

              Add {matchingApplication.displayName} to this behavior

            </button>
          </span>
        );
      } else {
        return (
          <span className="mhxs">
            <button
              type="button"
              className="button-raw link button-s"
              onClick={this.onNewOAuth2Application.bind(this, detail, detail.requiredApiConfig.id)}>

              Configure the {detail.name} API for this behavior

            </button>
          </span>
        );
      }
    },

    recommendedScopeAnnotation: function(detail) {
      var recommendedScope = detail.requiredApiConfig.recommendedScope;
      if (recommendedScope) {
        return (
          <span>(recommended scope: <b>{recommendedScope}</b>)</span>
        );
      }
    },

    getNotificationForMissingOAuth2Application: function() {
      var numRequiredApiConfigs = this.props.details.length;
      if (numRequiredApiConfigs === 1) {
        var detail = this.props.details[0];
        return (
          <span>
            <span>This behavior needs to be configured to use the <b>{detail.name}</b> API {this.recommendedScopeAnnotation(detail)}.</span>
            {this.addOAuth2ApplicationPrompt(detail)}
          </span>
        );
      } else {
        return (
          <span>
            <span>This behavior needs to be configured to use the following APIs: </span>
            {this.props.details.map((ea, index) => {
              return (
                <span key={"oAuthNotificationDetail" + index}>
                  <span>{ea.name} {this.recommendedScopeAnnotation(ea)}</span>
                  {this.addOAuth2ApplicationPrompt(ea)}
                  <span>{index + 1 < numRequiredApiConfigs ? ", " : ""}</span>
                </span>
              );
            })}
          </span>
        );
      }
    },

    getNotificationForUnusedOAuth2Application: function() {
      var numApps = this.props.details.length;
      if (numApps === 1) {
        var firstApp = this.props.details[0];
        return (
          <span>
            <span>Add <code className="box-code-example mhxs">{firstApp.code}</code> to your function to use the </span>
            <span>API token for <b>{firstApp.name}</b>. </span>
          </span>
        );
      } else {
        return (
          <span>
            <span>This behavior has the following API tokens available: </span>
            {this.props.details.map((detail, index) => {
              return (
                <span key={"oAuthNotificationDetail" + index}>
                  <code className="box-code-example mhxs">{detail.code}</code>
                  <span>{index + 1 < numApps ? ", " : ""}</span>
                </span>
              );
            })}
          </span>
        );
      }
    },

    getNotificationForUnusedAWS: function() {
      return (
        <span>
          <span>Use <code className="box-code-example mhxs">{this.props.details[0].code}</code> in your </span>
          <span>function to access methods and properties of the </span>
          <span><a href="http://docs.aws.amazon.com/AWSJavaScriptSDK/guide/node-intro.html" target="_blank">AWS SDK</a>.</span>
        </span>
      );
    },

    onClick: function(notificationDetail) {
      if (this.props.onClick) {
        this.props.onClick(notificationDetail);
      }
    },

    render: function() {
      var notification = this.getNotificationForKind(this.props.kind);
      return (
        <Collapsible revealWhen={!this.props.hidden} animateInitialRender={true}>
          <div className={"type-s phn position-z-above " + notification.containerClass} style={{ marginTop: -1 }}>
            <div className="container">
              {notification.icon}
              {notification.message}
            </div>
          </div>
        </Collapsible>
      );
    }
  });
});
