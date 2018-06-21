import * as React from 'react';
import TestResultNotificationData from "../models/notifications/test_result_notification_data";
import autobind from '../lib/autobind';

interface Props {
  details: Array<TestResultNotificationData>
}

class NotificationForTestResult extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  render() {
    return (
      <span>
        <span className="type-label">Warning: </span>
        <span className="mrs">You have some failing tests</span>
      </span>
    );
  }
}

export default NotificationForTestResult;
