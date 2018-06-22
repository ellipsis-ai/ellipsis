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
    const detail = this.props.details.find((ea) => ea.type === "test_failures");
    return (
      <span>
        <span className="type-label">Warning: </span>
        <span className="mrs">You have some failing tests</span>
        {detail ? (
          <button className="button-s button-shrink" type="button" onClick={detail.onClick}>Go to first failure</button>
        ) : null}
      </span>
    );
  }
}

export default NotificationForTestResult;
