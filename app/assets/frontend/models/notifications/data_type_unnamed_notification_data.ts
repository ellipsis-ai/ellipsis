import NotificationData from "../notification_data";

interface DataTypeUnnamedNotificationDataInterface extends NotificationData {
  kind: "data_type_unnamed";
  name: string;
  onClick: () => void;
}

class DataTypeUnnamedNotificationData extends NotificationData implements DataTypeUnnamedNotificationDataInterface {
  readonly kind: "data_type_unnamed";
  readonly name: string;
  readonly onClick: () => void;
  constructor(props: DataTypeUnnamedNotificationDataInterface) {
    super(props);
  }
}

export default DataTypeUnnamedNotificationData;
