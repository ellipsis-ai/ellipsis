import NotificationData, {NotificationDataInterface} from "../notification_data";

interface SkillDetailsWarningNotificationDataInterface extends NotificationDataInterface {
  type: string;
  onClick: () => void;
}

class SkillDetailsWarningNotificationData extends NotificationData implements SkillDetailsWarningNotificationDataInterface {
  readonly type: string;
  readonly onClick: () => void;
  constructor(props: SkillDetailsWarningNotificationDataInterface) {
    super(props, "skill_details_warning");
  }
}

export default SkillDetailsWarningNotificationData;
