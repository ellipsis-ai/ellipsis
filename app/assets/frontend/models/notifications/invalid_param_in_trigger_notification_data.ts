import NotificationData, {NotificationDataInterface} from "./notification_data";

interface InvalidParamInTriggerNotificationDataInterface extends NotificationDataInterface {
  name: string;
}

class InvalidParamInTriggerNotificationData extends NotificationData implements InvalidParamInTriggerNotificationDataInterface {
  readonly name: string;
  constructor(props: InvalidParamInTriggerNotificationDataInterface) {
    super(props, "invalid_param_in_trigger");
  }
}

export default InvalidParamInTriggerNotificationData;
