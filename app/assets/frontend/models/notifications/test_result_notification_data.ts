import NotificationData, {NotificationDataInterface} from "./notification_data";

interface TestResultNotificationDataInterface extends NotificationDataInterface {
  type: string;
  onClick: () => void;
}

class TestResultNotificationData extends NotificationData implements TestResultNotificationDataInterface {
  readonly type: string;
  readonly onClick: () => void;
  constructor(props: TestResultNotificationDataInterface) {
    super(props, "test_result");
  }
}

export default TestResultNotificationData;
