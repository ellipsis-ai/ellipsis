import NotificationData, {NotificationDataInterface} from "./notification_data";

interface DataTypeMissingFieldsNotificationDataInterface extends NotificationDataInterface {
  name: string;
  onClick: () => void;
}

class DataTypeMissingFieldsNotificationData extends NotificationData implements DataTypeMissingFieldsNotificationDataInterface {
  readonly name: string;
  readonly onClick: () => void;
  constructor(props: DataTypeMissingFieldsNotificationDataInterface) {
    super(props, "data_type_missing_fields");
  }
}

export default DataTypeMissingFieldsNotificationData;
