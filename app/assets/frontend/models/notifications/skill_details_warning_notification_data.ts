import NotificationData from "../notification_data";

interface SkillDetailsWarningNotificationDataInterface extends NotificationData {
  kind: "skill_details_warning";
  type: string;
  onClick: () => void;
}

class SkillDetailsWarningNotificationData extends NotificationData implements SkillDetailsWarningNotificationDataInterface {
  readonly kind: "skill_details_warning";
  readonly type: string;
  readonly onClick: () => void;
  constructor(props: SkillDetailsWarningNotificationDataInterface) {
    super(props);
  }
}

export default SkillDetailsWarningNotificationData;
