import NotificationData from "../notification_data";

interface DataTypeMissingFieldsNotificationDataInterface extends NotificationData {
  kind: "data_type_missing_fields";
  name: string;
  onClick: () => void;
}

class DataTypeMissingFieldsNotificationData extends NotificationData implements DataTypeMissingFieldsNotificationDataInterface {
  readonly kind: "data_type_missing_fields";
  readonly name: string;
  readonly onClick: () => void;
  constructor(props: DataTypeMissingFieldsNotificationDataInterface) {
    super(props);
  }
}

export default DataTypeMissingFieldsNotificationData;
