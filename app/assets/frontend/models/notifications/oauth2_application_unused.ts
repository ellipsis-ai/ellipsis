import NotificationData from "../notification_data";

interface OAuth2ApplicationUnusedNotificationDataInterface extends NotificationData {
  kind: "oauth2_application_unused";
  name: string;
  code: string;
}

class OAuth2ApplicationUnusedNotificationData extends NotificationData implements OAuth2ApplicationUnusedNotificationDataInterface {
  readonly kind: "oauth2_application_unused";
  readonly name: string;
  readonly code: string;
  constructor(props: OAuth2ApplicationUnusedNotificationDataInterface) {
    super(props);
  }
}

export default OAuth2ApplicationUnusedNotificationData;
