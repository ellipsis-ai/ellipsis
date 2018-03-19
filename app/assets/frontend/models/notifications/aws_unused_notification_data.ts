import NotificationData from "../notification_data";

interface AWSUnusedNotificationDataInterface extends NotificationData {
  kind: "aws_unused";
  code: string;
}

class AWSUnusedNotificationData extends NotificationData implements AWSUnusedNotificationDataInterface {
  readonly kind: "aws_unused";
  readonly code: string;
  constructor(props: AWSUnusedNotificationDataInterface) {
    super(props);
  }
}

export default AWSUnusedNotificationData;
