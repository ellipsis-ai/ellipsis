import * as React from 'react';
import AWSUnusedNotificationData from "../models/notifications/aws_unused_notification_data";
import autobind from '../lib/autobind';

interface Props {
  details: Array<AWSUnusedNotificationData>
}

class NotificationForUnusedAWS extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

    render() {
      return (
        <span>
          <span>Use <code className="box-code-example mhxs">{this.props.details[0].code}</code> in your </span>
          <span>function to access methods and properties of the </span>
          <span><a href="http://docs.aws.amazon.com/AWSJavaScriptSDK/guide/node-intro.html" target="_blank" rel="noreferrer noopener">AWS SDK</a>.</span>
        </span>
      );
    }
}

export default NotificationForUnusedAWS;
