define(function(require) {
  var React = require('react'),
    Collapsible = require('../collapsible'),
    SVGTip = require('../svg/tip'),
    SVGWarning = require('../svg/warning'),
    NotificationForEnvVarMissing = require('./env_var_not_defined'),
    NotificationForMissingOAuth2Application = require('./oauth2_config_without_application'),
    NotificationForUnusedOAuth2Application = require('./oauth2_application_unused'),
    NotificationForUnusedAWS = require('./aws_unused'),
    NotificationForParamNotInFunction = require('./param_not_in_function');

  return React.createClass({
    propTypes: {
      details: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      kind: React.PropTypes.string.isRequired,
      index: React.PropTypes.number.isRequired,
      hidden: React.PropTypes.bool
    },

    getNotificationForKind: function(kind) {
      if (kind === "env_var_not_defined") {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForEnvVarMissing details={this.props.details} />
          )
        };
      } else if (kind === "oauth2_config_without_application") {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForMissingOAuth2Application details={this.props.details} />
          )
        };
      } else if (kind === "oauth2_application_unused") {
        return {
          containerClass: "box-tip",
          icon: this.getTipIcon(),
          message: (
            <NotificationForUnusedOAuth2Application details={this.props.details} />
          )
        };
      } else if (kind === "aws_unused") {
        return {
          containerClass: "box-tip",
          icon: this.getTipIcon(),
          message: (
            <NotificationForUnusedAWS details={this.props.details} />
          )
        };
      } else if (kind === "param_not_in_function") {
        return {
          containerClass: "box-tip",
          icon: this.getTipIcon(),
          message: (
            <NotificationForParamNotInFunction details={this.props.details} />
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
