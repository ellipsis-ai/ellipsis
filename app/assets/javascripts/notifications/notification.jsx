define(function(require) {
  var React = require('react'),
    Collapsible = require('../collapsible'),
    SVGTip = require('../svg/tip'),
    SVGWarning = require('../svg/warning'),
    NotificationForEnvVarMissing = require('./env_var_not_defined'),
    NotificationForMissingOAuth2Application = require('./oauth2_config_without_application'),
    NotificationForUnusedOAuth2Application = require('./oauth2_application_unused'),
    NotificationForUnusedAWS = require('./aws_unused');

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
          message: (
            <NotificationForEnvVarMissing details={this.props.details} onClick={this.onClick} />
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
          message: this.getNotificationForParamsNotInFunction()
        };
      } else if (kind === "param_without_function") {
        return {
          containerClass: "box-tip",
          icon: this.getTipIcon(),
          message: this.getNotificationForParamsWithoutFunction()
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

    getNotificationForParamsNotInFunction: function() {
      var numParams = this.props.details.length;
      if (numParams === 1) {
        let detail = this.props.details[0];
        return (
          <span>
            <span>You’ve added a parameter in your triggers. Now add it to your </span>
            <span>function to use it in code: </span>
            <button type="button"
              className="button-raw type-monospace"
              onClick={this.onClick.bind(this, detail)}
            >{detail.name}</button>
          </span>
        );
      } else {
        return (
          <span>
            <span>You’ve added some parameters in your triggers. Now add them to your </span>
            <span>function to use them in code: </span>
              {this.props.details.map((detail, index) => (
                <span key={`unusedParamName${index}`}>
                  <button type="button"
                    className="button-raw type-monospace"
                    onClick={this.onClick.bind(this, detail)}
                  >{detail.name}</button>
                  <span>{index + 1 < numParams ? ", " : ""}</span>
                </span>
              ))}
          </span>
        );
      }
    },

    getNotificationForParamsWithoutFunction: function() {
      var paramNames = this.props.details.map((ea) => ea.name);
      return (
        <span>
          <span>If your behavior is going to run code, the function can receive any </span>
          <span>trigger fill-in-the-blanks as parameters. </span>
          <button type="button"
            className="button-raw"
            onClick={this.onClick.bind(this, {
              kind: "param_without_function",
              paramNames: paramNames
            })}
          >Add code with parameters</button>
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
