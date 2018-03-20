export type NotificationKind = "env_var_not_defined" |
  "required_aws_config_without_config" |
  "oauth2_config_without_application" |
  "data_type_needs_config" |
  "data_type_unnamed" |
  "data_type_missing_fields" |
  "data_type_unnamed_fields" |
  "data_type_duplicate_fields" |
  "oauth2_application_unused" |
  "aws_unused" |
  "param_not_in_function" |
  "unknown_param_in_template" |
  "invalid_param_in_trigger" |
  "server_data_warning" |
  "skill_details_warning"

export interface NotificationDataInterface {}

abstract class NotificationData implements NotificationDataInterface {
  readonly kind: NotificationKind;

  constructor(props: NotificationDataInterface, kind: NotificationKind) {
    Object.assign(this, props);
    this.kind = kind;
  }
}

export default NotificationData;
