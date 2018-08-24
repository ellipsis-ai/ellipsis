import * as React from 'react';
import {ReactElement} from "react";
import Collapsible from '../shared_ui/collapsible';
import SVGTip from '../svg/tip';
import SVGWarning from '../svg/warning';
import NotificationForEnvVarMissing from './env_var_not_defined';
import NotificationForRequiredAWSConfigWithoutConfig from './required_aws_config_without_config';
import NotificationForMissingOAuth1Application from './oauth1_config_without_application';
import NotificationForMissingOAuth2Application from './oauth2_config_without_application';
import NotificationForDataTypeNeedsConfig from './data_type_needs_config';
import NotificationForDataTypeUnnamed from './data_type_unnamed';
import NotificationForDataTypeMissingFields from './data_type_missing_fields';
import NotificationForDataTypeUnnamedFields from './data_type_unnamed_fields';
import NotificationForDataTypeDuplicateFields from './data_type_duplicate_fields';
import NotificationForUnusedOAuth1Application from './oauth1_application_unused';
import NotificationForUnusedOAuth2Application from './oauth2_application_unused';
import NotificationForUnusedAWS from './aws_unused';
import NotificationForParamNotInFunction from './param_not_in_function';
import NotificationForInvalidParamInTrigger from './invalid_param_in_trigger';
import NotificationForUnknownParamInTemplate from './unknown_param_in_template';
import NotificationForServerDataWarning from './server_data_warning';
import NotificationForSkillDetailsWarning from './skill_details_warning';
import NotificationForTestResultWarning from './test_result';
import NotificationDataGroup from '../models/notifications/notification_data_group';
import NotificationData, {NotificationDataInterface, NotificationKind} from "../models/notifications/notification_data";
import EnvVarMissingNotificationData from "../models/notifications/env_var_missing_notification_data";
import RequiredAwsConfigNotificationData from "../models/notifications/required_aws_config_notification_data";
import OAuth1ConfigWithoutApplicationNotificationData from "../models/notifications/oauth1_config_without_application_notification_data";
import OAuth2ConfigWithoutApplicationNotificationData from "../models/notifications/oauth2_config_without_application_notification_data";
import DataTypeNeedsConfigNotificationData from "../models/notifications/data_type_needs_config_notification_data";
import DataTypeUnnamedNotificationData from "../models/notifications/data_type_unnamed_notification_data";
import DataTypeMissingFieldsNotificationData from "../models/notifications/data_type_missing_fields_notification_data";
import DataTypeUnnamedFieldsNotificationData from "../models/notifications/data_type_unnamed_fields_notification_data";
import DataTypeDuplicateFieldsNotificationData from "../models/notifications/data_type_duplicate_fields_notification_data";
import OAuth1ApplicationUnusedNotificationData from "../models/notifications/oauth1_application_unused";
import OAuth2ApplicationUnusedNotificationData from "../models/notifications/oauth2_application_unused";
import AWSUnusedNotificationData from "../models/notifications/aws_unused_notification_data";
import ParamNotInFunctionNotificationData from "../models/notifications/param_not_in_function_notification_data";
import InvalidParamInTriggerNotificationData from "../models/notifications/invalid_param_in_trigger_notification_data";
import UnknownParamInTemplateNotificationData from "../models/notifications/unknown_param_in_template_notification_data";
import ServerDataWarningNotificationData from "../models/notifications/server_data_warning_notification_data";
import SkillDetailsWarningNotificationData from "../models/notifications/skill_details_warning_notification_data";
import TestResultNotificationData from "../models/notifications/test_result_notification_data";

interface Props<T extends NotificationData> {
  group: NotificationDataGroup<T>,
  inline?: Option<boolean>
}

interface NotificationConfig {
  containerClass: string,
  icon: ReactElement<any>,
  message: ReactElement<any>
}

function notificationTypeIs<I extends NotificationDataInterface, T extends NotificationData>(arr: Array<NotificationData>, type: new(props: I) => T): arr is Array<T> {
  return arr.every(ea => ea instanceof type);
}

class Notification<T extends NotificationData> extends React.Component<Props<T>> {
    getNotificationForKind(kind: NotificationKind, members: Array<NotificationData>): NotificationConfig {
      if (notificationTypeIs(members, EnvVarMissingNotificationData)) {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForEnvVarMissing details={members}/>
          )
        };
      } else if (notificationTypeIs(members, RequiredAwsConfigNotificationData)) {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForRequiredAWSConfigWithoutConfig details={members} />
          )
        };
      } else if (notificationTypeIs(members, OAuth1ConfigWithoutApplicationNotificationData)) {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForMissingOAuth1Application details={members} />
          )
        };
      } else if (notificationTypeIs(members, OAuth2ConfigWithoutApplicationNotificationData)) {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForMissingOAuth2Application details={members} />
          )
        };
      } else if (notificationTypeIs(members, DataTypeNeedsConfigNotificationData)) {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForDataTypeNeedsConfig details={members} />
          )
        };
      } else if (notificationTypeIs(members, DataTypeUnnamedNotificationData)) {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForDataTypeUnnamed details={members} />
          )
        };
      } else if (notificationTypeIs(members, DataTypeMissingFieldsNotificationData)) {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForDataTypeMissingFields details={members} />
          )
        };
      } else if (notificationTypeIs(members, DataTypeUnnamedFieldsNotificationData)) {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForDataTypeUnnamedFields details={members} />
          )
        };
      } else if (notificationTypeIs(members, DataTypeDuplicateFieldsNotificationData)) {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForDataTypeDuplicateFields details={members} />
          )
        };
      } else if (notificationTypeIs(members, OAuth1ApplicationUnusedNotificationData)) {
        return {
          containerClass: "box-tip pvs",
          icon: this.getTipIcon(),
          message: (
            <NotificationForUnusedOAuth1Application details={members} />
          )
        };
      } else if (notificationTypeIs(members, OAuth2ApplicationUnusedNotificationData)) {
        return {
          containerClass: "box-tip pvs",
          icon: this.getTipIcon(),
          message: (
            <NotificationForUnusedOAuth2Application details={members} />
          )
        };
      } else if (notificationTypeIs(members, AWSUnusedNotificationData)) {
        return {
          containerClass: "box-tip pvs",
          icon: this.getTipIcon(),
          message: (
            <NotificationForUnusedAWS details={members} />
          )
        };
      } else if (notificationTypeIs(members, ParamNotInFunctionNotificationData)) {
        return {
          containerClass: "box-tip",
          icon: this.getTipIcon(),
          message: (
            <NotificationForParamNotInFunction details={members} />
          )
        };
      } else if (notificationTypeIs(members, UnknownParamInTemplateNotificationData)) {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForUnknownParamInTemplate details={members} />
          )
        };
      } else if (notificationTypeIs(members, InvalidParamInTriggerNotificationData)) {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForInvalidParamInTrigger details={members}/>
          )
        };
      } else if (notificationTypeIs(members, ServerDataWarningNotificationData)) {
        return {
          containerClass: "box-error",
          icon: this.getErrorIcon(),
          message: (
            <NotificationForServerDataWarning details={members} />
          )
        };
      } else if (notificationTypeIs(members, SkillDetailsWarningNotificationData)) {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForSkillDetailsWarning details={members} />
          )
        };
      } else if (notificationTypeIs(members, TestResultNotificationData)) {
        return {
          containerClass: "box-warning",
          icon: this.getWarningIcon(),
          message: (
            <NotificationForTestResultWarning details={members} />
          )
        };
      } else {
        throw new Error(`Error: ${kind} is not a valid notification`);
      }
    }

    getWarningIcon() {
      return (
        <span className="display-inline-block align-b type-yellow" style={{ width: 22, height: 24 }}>
          <SVGWarning />
        </span>
      );
    }

    getErrorIcon() {
      return (
        <span className="display-inline-block align-b type-pink" style={{ width: 22, height: 24 }}>
          <SVGWarning inverted={true} />
        </span>
      );
    }

    getTipIcon() {
      return (
        <span className="display-inline-block align-b type-green" style={{ width: 22, height: 24 }}>
          <SVGTip />
        </span>
      );
    }

    render() {
      var notification = this.getNotificationForKind(this.props.group.kind, this.props.group.members);
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
}

export default Notification;
