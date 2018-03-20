import NotificationData, {NotificationDataInterface} from "../notification_data";

interface EnvVarMissingNotificationDataInterface extends NotificationDataInterface {
  environmentVariableName: string;
  onClick: () => void;
}

class EnvVarMissingNotificationData extends NotificationData implements EnvVarMissingNotificationDataInterface {
  readonly environmentVariableName: string;
  readonly onClick: () => void;
  constructor(props: EnvVarMissingNotificationDataInterface) {
    super(props, "env_var_not_defined");
  }
}

export default EnvVarMissingNotificationData;
