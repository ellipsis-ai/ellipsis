import NotificationData, {NotificationDataInterface} from "../notification_data";

interface AWSUnusedNotificationDataInterface extends NotificationDataInterface {
  code: string;
}

class AWSUnusedNotificationData extends NotificationData implements AWSUnusedNotificationDataInterface {
  readonly code: string;
  constructor(props: AWSUnusedNotificationDataInterface) {
    super(props, "aws_unused");
  }
}

export default AWSUnusedNotificationData;
