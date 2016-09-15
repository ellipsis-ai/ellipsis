define(function(require) {
  var React = require('react'),
    Collapsible = require('../collapsible'),
    SVGTip = require('../svg/tip'),
    SVGWarning = require('../svg/warning'),
    NotificationForEnvVarMissing = require('./env_var_missing'),
    NotificationForMissingOAuth2Application = require('./missing_oauth2_application');

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
          message: this.getNotificationForUnusedOAuth2Application()
        };
      } else if (kind === "aws_unused") {
        return {
          containerClass: "box-tip",
          icon: this.getTipIcon(),
          message: this.getNotificationForUnusedAWS()
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
