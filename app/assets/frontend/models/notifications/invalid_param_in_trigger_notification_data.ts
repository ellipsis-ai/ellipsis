import NotificationData from "../notification_data";

interface InvalidParamInTriggerNotificationDataInterface extends NotificationData {
  kind: "invalid_param_in_trigger";
  name: string;
}

class InvalidParamInTriggerNotificationData extends NotificationData implements InvalidParamInTriggerNotificationDataInterface {
  readonly kind: "invalid_param_in_trigger";
  readonly name: string;
  constructor(props: InvalidParamInTriggerNotificationDataInterface) {
    super(props);
  }
}

export default InvalidParamInTriggerNotificationData;
