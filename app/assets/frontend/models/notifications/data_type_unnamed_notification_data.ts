import NotificationData, {NotificationDataInterface} from "./notification_data";

interface DataTypeUnnamedNotificationDataInterface extends NotificationDataInterface {
  onClick: () => void;
}

class DataTypeUnnamedNotificationData extends NotificationData implements DataTypeUnnamedNotificationDataInterface {
  readonly onClick: () => void;
  constructor(props: DataTypeUnnamedNotificationDataInterface) {
    super(props, "data_type_unnamed");
  }
}

export default DataTypeUnnamedNotificationData;
