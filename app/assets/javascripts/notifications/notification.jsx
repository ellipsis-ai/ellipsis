define(function(require) {
  var React = require('react'),
    Collapsible = require('../shared_ui/collapsible'),
    SVGTip = require('../svg/tip'),
    SVGWarning = require('../svg/warning'),
    NotificationForEnvVarMissing = require('./env_var_not_defined'),
    NotificationForRequiredAWSConfigWithoutConfig = require('./required_aws_config_without_config'),
    NotificationForMissingOAuth2Application = require('./oauth2_config_without_application'),
    NotificationForDataTypeNeedsConfig = require('./data_type_needs_config'),
    NotificationForDataTypeUnnamed = require('./data_type_unnamed'),
    NotificationForDataTypeMissingFields = require('./data_type_missing_fields'),
    NotificationForDataTypeUnnamedFields = require('./data_type_unnamed_fields'),
    NotificationForDataTypeDuplicateFields = require('./data_type_duplicate_fields'),
    NotificationForUnusedOAuth2Application = require('./oauth2_application_unused'),
    NotificationForUnusedAWS = require('./aws_unused'),
    NotificationForParamNotInFunction = require('./param_not_in_function'),
    NotificationForInvalidParamInTrigger = require('./invalid_param_in_trigger'),
    NotificationForUnknownParamInTemplate = require('./unknown_param_in_template'),
    NotificationForServerDataWarning = require('./server_data_warning'),
    NotificationForDeploymentWarning = require('./deployment_warning'),
    NotificationDataGroup = require('../models/notification_data_group');

  return React.createClass({
    displayName: 'Notification',
    propTypes: {
      group: React.PropTypes.instanceOf(NotificationDataGroup).isRequired,
      inline: React.PropTypes.bool
    },

    getNotificationForKind: function(kind) {
      if (kind === "env_var_not_defined") {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForEnvVarMissing details={this.props.group.members}/>
          )
        };
      } else if (kind === "required_aws_config_without_config") {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForRequiredAWSConfigWithoutConfig details={this.props.group.members} />
          )
        };
      } else if (kind === "oauth2_config_without_application") {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForMissingOAuth2Application details={this.props.group.members} />
          )
        };
      } else if (kind === 'data_type_needs_config') {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForDataTypeNeedsConfig details={this.props.group.members} />
          )
        };
      } else if (kind === "data_type_unnamed") {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForDataTypeUnnamed details={this.props.group.members} />
          )
        };
      } else if (kind === "data_type_missing_fields") {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForDataTypeMissingFields details={this.props.group.members} />
          )
        };
      } else if (kind === "data_type_unnamed_fields") {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForDataTypeUnnamedFields details={this.props.group.members} />
          )
        };
      } else if (kind === "data_type_duplicate_fields") {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForDataTypeDuplicateFields details={this.props.group.members} />
          )
        };
      } else if (kind === "oauth2_application_unused") {
        return {
          containerClass: "box-tip pvs",
          icon: this.getTipIcon(),
          message: (
            <NotificationForUnusedOAuth2Application details={this.props.group.members} />
          )
        };
      } else if (kind === "aws_unused") {
        return {
          containerClass: "box-tip pvs",
          icon: this.getTipIcon(),
          message: (
            <NotificationForUnusedAWS details={this.props.group.members} />
          )
        };
      } else if (kind === "param_not_in_function") {
        return {
          containerClass: "box-tip",
          icon: this.getTipIcon(),
          message: (
            <NotificationForParamNotInFunction details={this.props.group.members} />
          )
        };
      } else if (kind === "unknown_param_in_template") {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForUnknownParamInTemplate details={this.props.group.members} />
          )
        };
      } else if (kind === "invalid_param_in_trigger") {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForInvalidParamInTrigger details={this.props.group.members}/>
          )
        };
      } else if (kind === "server_data_warning") {
        return {
          containerClass: "box-error",
          icon: this.getErrorIcon(),
          message: (
            <NotificationForServerDataWarning details={this.props.group.members} />
          )
        };
      } else if (kind === "deployment_warning") {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForDeploymentWarning details={this.props.group.members} />
          )
        };
      } else {
        throw new Error(`Error: ${kind} is not a valid notification`);
      }
    },

    getWarningIcon: function() {
      return (
        <span className="display-inline-block align-b type-yellow" style={{ width: 22, height: 24 }}>
          <SVGWarning />
        </span>
      );
    },

    getErrorIcon: function() {
      return (
        <span className="display-inline-block align-b type-pink" style={{ width: 22, height: 24 }}>
          <SVGWarning inverted={true} />
        </span>
      );
    },

    getTipIcon: function() {
      return (
        <span className="display-inline-block align-b type-green" style={{ width: 22, height: 24 }}>
          <SVGTip />
        </span>
      );
    },

    render: function() {
      var notification = this.getNotificationForKind(this.props.group.kind);
      return (
        <Collapsible revealWhen={!this.props.group.hidden} animateInitialRender={true}>
          <div className={
            "type-s phn position-z-above mbneg1 " +
            (this.props.inline ? " border-left border-right " : "") +
            notification.containerClass
          }>
            <div className={this.props.inline ? "phs" : "container"}>
              <div className="columns columns-elastic">
                <div className="column column-shrink prs">{notification.icon}</div>
                <div className="column column-expand">{notification.message}</div>
              </div>
            </div>
          </div>
        </Collapsible>
      );
    }
  });
});
