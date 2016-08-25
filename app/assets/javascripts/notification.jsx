define(function(require) {
  var React = require('react'),
    SVGWarning = require('./svg/warning');

  return React.createClass({
    propTypes: {
      details: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      kind: React.PropTypes.string.isRequired,
      index: React.PropTypes.number.isRequired,
      onClick: React.PropTypes.func.isRequired
    },

    getNotificationForKind: function(kind) {
      if (kind === "env_var_not_defined") {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: this.getNotificationForEnvVarMissing()
        };
      } else if (kind === "oauth2_application_unused") {
        return {
          containerClass: "box-tip",
          icon: this.getTipIcon(),
          message: this.getNotificationForUnusedOAuth2Application()
        };
      }
    },

    getWarningIcon: function() {
      return (
        <span className="display-inline-block mrs align-b type-yellow" style={{ height: 24 }}>
          <SVGWarning />
        </span>
      );
    },

    getTipIcon: function() {
      return (
        <span className="display-inline-block mrs align-m type-pink type-l" style={{ height: 24 }}>
          ☞
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

    onClick: function(notificationDetail) {
      if (this.props.onClick) {
        this.props.onClick(notificationDetail);
      }
    },

    render: function() {
      var notification = this.getNotificationForKind(this.props.kind);
      return (
        <div className={"type-s phn position-z-above " + notification.containerClass} style={{ marginTop: -1 }}>
          <div className="container">
            {notification.icon}
            {notification.message}
          </div>
        </div>
      );
    }
  });
});
