import NotificationData, {NotificationDataInterface} from "./notification_data";

interface OAuthApplicationUnusedNotificationDataInterface extends NotificationDataInterface {
  name: string;
  code: string;
}

class OAuthApplicationUnusedNotificationData extends NotificationData implements OAuthApplicationUnusedNotificationDataInterface {
  readonly name: string;
  readonly code: string;
  constructor(props: OAuthApplicationUnusedNotificationDataInterface) {
    super(props, "oauth_application_unused");
  }
}

export default OAuthApplicationUnusedNotificationData;
