import NotificationData, {NotificationDataInterface} from "./notification_data";

interface OAuth1ApplicationUnusedNotificationDataInterface extends NotificationDataInterface {
  name: string;
  code: string;
}

class OAuth1ApplicationUnusedNotificationData extends NotificationData implements OAuth1ApplicationUnusedNotificationDataInterface {
  readonly name: string;
  readonly code: string;
  constructor(props: OAuth1ApplicationUnusedNotificationDataInterface) {
    super(props, "oauth1_application_unused");
  }
}

export default OAuth1ApplicationUnusedNotificationData;
