import NotificationData, {NotificationDataInterface} from "./notification_data";

interface OAuth2ApplicationUnusedNotificationDataInterface extends NotificationDataInterface {
  name: string;
  code: string;
}

class OAuth2ApplicationUnusedNotificationData extends NotificationData implements OAuth2ApplicationUnusedNotificationDataInterface {
  readonly name: string;
  readonly code: string;
  constructor(props: OAuth2ApplicationUnusedNotificationDataInterface) {
    super(props, "oauth2_application_unused");
  }
}

export default OAuth2ApplicationUnusedNotificationData;
