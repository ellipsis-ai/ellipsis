import NotificationData from "../notification_data";

interface DataTypeNeedsConfigNotificationDataInterface extends NotificationData {
  kind: "data_type_needs_config";
  name: string;
  onClick: () => void;
}

class DataTypeNeedsConfigNotificationData extends NotificationData implements DataTypeNeedsConfigNotificationDataInterface {
  readonly kind: "data_type_needs_config";
  readonly name: string;
  readonly onClick: () => void;
  constructor(props: DataTypeNeedsConfigNotificationDataInterface) {
    super(props);
  }
}

export default DataTypeNeedsConfigNotificationData;
