import NotificationData, {NotificationDataInterface} from "../notification_data";

interface ParamNotInFunctionNotificationDataInterface extends NotificationDataInterface {
  name: string;
  onClick: () => void;
}

class ParamNotInFunctionNotificationData extends NotificationData implements ParamNotInFunctionNotificationDataInterface {
  readonly name: string;
  readonly onClick: () => void;
  constructor(props: ParamNotInFunctionNotificationDataInterface) {
    super(props, "param_not_in_function");
  }
}

export default ParamNotInFunctionNotificationData;
