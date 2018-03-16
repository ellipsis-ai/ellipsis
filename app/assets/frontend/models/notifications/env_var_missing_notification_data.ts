import NotificationData from "../notification_data";

interface EnvVarMissingNotificationDataInterface extends NotificationData {
  kind: "env_var_not_defined";
  environmentVariableName: string;
  onClick: () => void;
}

class EnvVarMissingNotificationData extends NotificationData implements EnvVarMissingNotificationDataInterface {
  readonly kind: "env_var_not_defined";
  readonly environmentVariableName: string;
  readonly onClick: () => void;
  constructor(props: EnvVarMissingNotificationDataInterface) {
    super(props);
  }
}

export default EnvVarMissingNotificationData;
