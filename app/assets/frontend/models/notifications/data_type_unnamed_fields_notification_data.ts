import NotificationData from "../notification_data";

interface DataTypeUnnamedFieldsNotificationDataInterface extends NotificationData {
  kind: "data_type_unnamed_fields";
  name: string;
  onClick: () => void;
}

class DataTypeUnnamedFieldsNotificationData extends NotificationData implements DataTypeUnnamedFieldsNotificationDataInterface {
  readonly kind: "data_type_unnamed_fields";
  readonly name: string;
  readonly onClick: () => void;
  constructor(props: DataTypeUnnamedFieldsNotificationDataInterface) {
    super(props);
  }
}

export default DataTypeUnnamedFieldsNotificationData;
