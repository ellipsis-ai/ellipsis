import NotificationData, {NotificationDataInterface} from "./notification_data";
import {OAuthApplicationRef, RequiredOAuthApplication} from "../oauth";

interface OAuthConfigWithoutApplicationNotificationDataInterface extends NotificationDataInterface {
  name: string;
  existingOAuthApplications: Array<OAuthApplicationRef>;
  requiredApiConfig: RequiredOAuthApplication;
  onUpdateOAuthApplication: (updatedApp: RequiredOAuthApplication) => void;
  onNewOAuthApplication: (newApp: RequiredOAuthApplication) => void;
  onConfigClick: (configApp: RequiredOAuthApplication) => void;
}

class OAuthConfigWithoutApplicationNotificationData extends NotificationData implements OAuthConfigWithoutApplicationNotificationDataInterface {
  readonly name: string;
  readonly existingOAuthApplications: Array<OAuthApplicationRef>;
  readonly requiredApiConfig: RequiredOAuthApplication;
  readonly onUpdateOAuthApplication: (updatedApp: RequiredOAuthApplication) => void;
  readonly onNewOAuthApplication: (newApp: RequiredOAuthApplication) => void;
  readonly onConfigClick: (configApp: RequiredOAuthApplication) => void;
  constructor(props: OAuthConfigWithoutApplicationNotificationDataInterface) {
    super(props, "oauth_config_without_application");
  }
}

export default OAuthConfigWithoutApplicationNotificationData;
