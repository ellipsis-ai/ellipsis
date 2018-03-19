import NotificationData from "../notification_data";

interface UnknownParamInTemplateNotificationDataInterface extends NotificationData {
  kind: "unknown_param_in_template";
  name: string;
}

class UnknownParamInTemplateNotificationData extends NotificationData implements UnknownParamInTemplateNotificationDataInterface {
  readonly kind: "unknown_param_in_template";
  readonly name: string;
  constructor(props: UnknownParamInTemplateNotificationDataInterface) {
    super(props);
  }
}

export default UnknownParamInTemplateNotificationData;
