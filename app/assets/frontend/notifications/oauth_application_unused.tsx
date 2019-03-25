import * as React from 'react';
import OAuthApplicationUnusedNotificationData from "../models/notifications/oauth_application_unused";
import autobind from '../lib/autobind';

interface Props {
  details: Array<OAuthApplicationUnusedNotificationData>
}

class NotificationForUnusedOAuthApplication extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  render() {
    var numApps = this.props.details.length;
    if (numApps === 1) {
      var firstApp = this.props.details[0];
      return (
        <span>
            <span>Add <code className="box-code-example mhxs">{firstApp.code}</code> to your function to use the </span>
            <span>API token for <b>{firstApp.name}</b>. </span>
          </span>
      );
    } else {
      return (
        <span>
            <span>This skill has the following API tokens available: </span>
          {this.props.details.map((detail, index) => {
            return (
              <span key={"oAuthNotificationDetail" + index}>
                  <code className="box-code-example mhxs">{detail.code}</code>
                  <span>{index + 1 < numApps ? ", " : ""}</span>
                </span>
            );
          })}
          </span>
      );
    }
  }
}

export default NotificationForUnusedOAuthApplication;
