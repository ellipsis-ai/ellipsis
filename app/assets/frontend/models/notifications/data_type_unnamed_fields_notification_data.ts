import NotificationData, {NotificationDataInterface} from "./notification_data";

interface DataTypeUnnamedFieldsNotificationDataInterface extends NotificationDataInterface {
  name: string;
  onClick: () => void;
}

class DataTypeUnnamedFieldsNotificationData extends NotificationData implements DataTypeUnnamedFieldsNotificationDataInterface {
  readonly name: string;
  readonly onClick: () => void;
  constructor(props: DataTypeUnnamedFieldsNotificationDataInterface) {
    super(props, "data_type_unnamed_fields");
  }
}

export default DataTypeUnnamedFieldsNotificationData;
