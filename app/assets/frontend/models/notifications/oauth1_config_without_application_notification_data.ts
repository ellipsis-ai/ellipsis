import NotificationData, {NotificationDataInterface} from "./notification_data";
import {OAuth1ApplicationRef, RequiredOAuth1Application} from "../oauth1";

interface OAuth1ConfigWithoutApplicationNotificationDataInterface extends NotificationDataInterface {
  name: string;
  existingOAuth1Applications: Array<OAuth1ApplicationRef>;
  requiredApiConfig: RequiredOAuth1Application;
  onUpdateOAuth1Application: (updatedApp: RequiredOAuth1Application) => void;
  onNewOAuth1Application: (newApp: RequiredOAuth1Application) => void;
  onConfigClick: (configApp: RequiredOAuth1Application) => void;
}

class OAuth1ConfigWithoutApplicationNotificationData extends NotificationData implements OAuth1ConfigWithoutApplicationNotificationDataInterface {
  readonly name: string;
  readonly existingOAuth1Applications: Array<OAuth1ApplicationRef>;
  readonly requiredApiConfig: RequiredOAuth1Application;
  readonly onUpdateOAuth1Application: (updatedApp: RequiredOAuth1Application) => void;
  readonly onNewOAuth1Application: (newApp: RequiredOAuth1Application) => void;
  readonly onConfigClick: (configApp: RequiredOAuth1Application) => void;
  constructor(props: OAuth1ConfigWithoutApplicationNotificationDataInterface) {
    super(props, "oauth1_config_without_application");
  }
}

export default OAuth1ConfigWithoutApplicationNotificationData;
