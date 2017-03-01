define(function(require) {
  var React = require('react'),
    Collapsible = require('../shared_ui/collapsible'),
    SVGTip = require('../svg/tip'),
    SVGWarning = require('../svg/warning'),
    NotificationForEnvVarMissing = require('./env_var_not_defined'),
    NotificationForMissingOAuth2Application = require('./oauth2_config_without_application'),
    NotificationForDataTypeNeedsConfig = require('./data_type_needs_config'),
    NotificationForUnusedOAuth2Application = require('./oauth2_application_unused'),
    NotificationForUnusedAWS = require('./aws_unused'),
    NotificationForParamNotInFunction = require('./param_not_in_function'),
    NotificationForInvalidParamInTrigger = require('./invalid_param_in_trigger'),
    NotificationForUnknownParamInTemplate = require('./unknown_param_in_template');

  return React.createClass({
    displayName: 'Notification',
    propTypes: {
      notification: React.PropTypes.shape({
        details: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
        kind: React.PropTypes.string.isRequired,
        hidden: React.PropTypes.bool
      }).isRequired
    },

    getNotificationForKind: function(kind) {
      if (kind === "env_var_not_defined") {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForEnvVarMissing details={this.props.notification.details} />
          )
        };
      } else if (kind === "oauth2_config_without_application") {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForMissingOAuth2Application details={this.props.notification.details} />
          )
        };
      } else if (kind === 'data_type_needs_config') {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForDataTypeNeedsConfig details={this.props.notification.details} />
          )
        };
      } else if (kind === "oauth2_application_unused") {
        return {
          containerClass: "box-tip",
          icon: this.getTipIcon(),
          message: (
            <NotificationForUnusedOAuth2Application details={this.props.notification.details} />
          )
        };
      } else if (kind === "aws_unused") {
        return {
          containerClass: "box-tip",
          icon: this.getTipIcon(),
          message: (
            <NotificationForUnusedAWS details={this.props.notification.details} />
          )
        };
      } else if (kind === "param_not_in_function") {
        return {
          containerClass: "box-tip",
          icon: this.getTipIcon(),
          message: (
            <NotificationForParamNotInFunction details={this.props.notification.details} />
          )
        };
      } else if (kind === "unknown_param_in_template") {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForUnknownParamInTemplate details={this.props.notification.details} />
          )
        };
      } else if (kind === "invalid_param_in_trigger") {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForInvalidParamInTrigger details={this.props.notification.details} />
          )
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

    render: function() {
      var notification = this.getNotificationForKind(this.props.notification.kind);
      return (
        <Collapsible revealWhen={!this.props.notification.hidden} animateInitialRender={true}>
          <div className={"type-s phn position-z-above mbneg1 " + notification.containerClass}>
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
