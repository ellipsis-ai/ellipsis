import * as React from 'react';
import Collapsible from '../shared_ui/collapsible';
import SVGTip from '../svg/tip';
import SVGWarning from '../svg/warning';
import NotificationForEnvVarMissing from './env_var_not_defined';
import NotificationForRequiredAWSConfigWithoutConfig from './required_aws_config_without_config';
import NotificationForMissingOAuth2Application from './oauth2_config_without_application';
import NotificationForDataTypeNeedsConfig from './data_type_needs_config';
import NotificationForDataTypeUnnamed from './data_type_unnamed';
import NotificationForDataTypeMissingFields from './data_type_missing_fields';
import NotificationForDataTypeUnnamedFields from './data_type_unnamed_fields';
import NotificationForDataTypeDuplicateFields from './data_type_duplicate_fields';
import NotificationForUnusedOAuth2Application from './oauth2_application_unused';
import NotificationForUnusedAWS from './aws_unused';
import NotificationForParamNotInFunction from './param_not_in_function';
import NotificationForInvalidParamInTrigger from './invalid_param_in_trigger';
import NotificationForUnknownParamInTemplate from './unknown_param_in_template';
import NotificationForServerDataWarning from './server_data_warning';
import NotificationForSkillDetailsWarning from './skill_details_warning';
import NotificationForDeploymentWarning from './deployment_warning';
import NotificationDataGroup from '../models/notification_data_group';

const Notification = React.createClass({
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
          containerClass: "box-tip",
          icon: this.getTipIcon(),
          message: (
            <NotificationForDeploymentWarning details={this.props.group.members}/>
          )
        };
      } else if (kind === "skill_details_warning") {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForSkillDetailsWarning details={this.props.group.members} />
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

export default Notification;
