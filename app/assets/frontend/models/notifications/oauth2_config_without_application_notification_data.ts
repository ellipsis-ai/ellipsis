import NotificationData, {NotificationDataInterface} from "./notification_data";
import {OAuth2ApplicationRef, RequiredOAuth2Application} from "../oauth2";

interface OAuth2ConfigWithoutApplicationNotificationDataInterface extends NotificationDataInterface {
  name: string;
  existingOAuth2Applications: Array<OAuth2ApplicationRef>;
  requiredApiConfig: RequiredOAuth2Application;
  onUpdateOAuth2Application: (updatedApp: RequiredOAuth2Application) => void;
  onNewOAuth2Application: (newApp: RequiredOAuth2Application) => void;
  onConfigClick: (configApp: RequiredOAuth2Application) => void;
}

class OAuth2ConfigWithoutApplicationNotificationData extends NotificationData implements OAuth2ConfigWithoutApplicationNotificationDataInterface {
  readonly name: string;
  readonly existingOAuth2Applications: Array<OAuth2ApplicationRef>;
  readonly requiredApiConfig: RequiredOAuth2Application;
  readonly onUpdateOAuth2Application: (updatedApp: RequiredOAuth2Application) => void;
  readonly onNewOAuth2Application: (newApp: RequiredOAuth2Application) => void;
  readonly onConfigClick: (configApp: RequiredOAuth2Application) => void;
  constructor(props: OAuth2ConfigWithoutApplicationNotificationDataInterface) {
    super(props, "oauth2_config_without_application");
  }
}

export default OAuth2ConfigWithoutApplicationNotificationData;
