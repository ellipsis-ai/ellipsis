import NotificationData from "../notification_data";

interface ParamNotInFunctionNotificationDataInterface extends NotificationData {
  kind: "param_not_in_function";
  name: string;
  onClick: () => void;
}

class ParamNotInFunctionNotificationData extends NotificationData implements ParamNotInFunctionNotificationDataInterface {
  readonly kind: "param_not_in_function";
  readonly name: string;
  readonly onClick: () => void;
  constructor(props: ParamNotInFunctionNotificationDataInterface) {
    super(props);
  }
}

export default ParamNotInFunctionNotificationData;
