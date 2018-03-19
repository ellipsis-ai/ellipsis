import NotificationData from "../notification_data";

interface DataTypeDuplicateFieldsNotificationDataInterface extends NotificationData {
  kind: "data_type_duplicate_fields";
  name: string;
  onClick: () => void;
}

class DataTypeDuplicateFieldsNotificationData extends NotificationData implements DataTypeDuplicateFieldsNotificationDataInterface {
  readonly kind: "data_type_duplicate_fields";
  readonly name: string;
  readonly onClick: () => void;
  constructor(props: DataTypeDuplicateFieldsNotificationDataInterface) {
    super(props);
  }
}

export default DataTypeDuplicateFieldsNotificationData;
