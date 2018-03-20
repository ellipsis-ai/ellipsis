import NotificationData, {NotificationDataInterface} from "../notification_data";

interface DataTypeDuplicateFieldsNotificationDataInterface extends NotificationDataInterface {
  name: string;
  onClick: () => void;
}

class DataTypeDuplicateFieldsNotificationData extends NotificationData implements DataTypeDuplicateFieldsNotificationDataInterface {
  readonly name: string;
  readonly onClick: () => void;
  constructor(props: DataTypeDuplicateFieldsNotificationDataInterface) {
    super(props, "data_type_duplicate_fields");
  }
}

export default DataTypeDuplicateFieldsNotificationData;
