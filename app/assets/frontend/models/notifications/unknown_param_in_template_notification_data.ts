import NotificationData, {NotificationDataInterface} from "../notification_data";

interface UnknownParamInTemplateNotificationDataInterface extends NotificationDataInterface {
  name: string;
}

class UnknownParamInTemplateNotificationData extends NotificationData implements UnknownParamInTemplateNotificationDataInterface {
  readonly name: string;
  constructor(props: UnknownParamInTemplateNotificationDataInterface) {
    super(props, "unknown_param_in_template");
  }
}

export default UnknownParamInTemplateNotificationData;
