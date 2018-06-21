import NotificationData, {NotificationDataInterface} from "./notification_data";

interface TestResultNotificationDataInterface extends NotificationDataInterface {
  type: string;
}

class TestResultNotificationData extends NotificationData implements TestResultNotificationDataInterface {
  readonly type: string;
  constructor(props: TestResultNotificationDataInterface) {
    super(props, "test_result");
  }
}

export default TestResultNotificationData;
