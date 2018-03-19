import NotificationData from "../notification_data";
import BehaviorGroupVersionMetaData from "../behavior_group_version_meta_data";

interface ServerDataWarningNotificationDataInterface extends NotificationData {
  kind: "server_data_warning";
  type: string;
  onClick?: Option<() => void>;
  error?: Option<Error>;
  currentUserId?: Option<string>;
  newerVersion?: Option<BehaviorGroupVersionMetaData>;
}

class ServerDataWarningNotificationData extends NotificationData implements ServerDataWarningNotificationDataInterface {
  readonly kind: "server_data_warning";
  readonly type: string;
  readonly onClick?: Option<() => void>;
  readonly error?: Option<Error>;
  readonly currentUserId?: Option<string>;
  readonly newerVersion?: Option<BehaviorGroupVersionMetaData>;
  constructor(props: ServerDataWarningNotificationDataInterface) {
    super(props);
  }
}

export default ServerDataWarningNotificationData;
