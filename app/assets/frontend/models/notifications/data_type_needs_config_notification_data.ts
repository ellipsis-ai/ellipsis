import NotificationData, {NotificationDataInterface} from "./notification_data";

interface DataTypeNeedsConfigNotificationDataInterface extends NotificationDataInterface {
  name: string;
  onClick: () => void;
}

class DataTypeNeedsConfigNotificationData extends NotificationData implements DataTypeNeedsConfigNotificationDataInterface {
  readonly name: string;
  readonly onClick: () => void;
  constructor(props: DataTypeNeedsConfigNotificationDataInterface) {
    super(props, "data_type_needs_config");
  }
}

export default DataTypeNeedsConfigNotificationData;
