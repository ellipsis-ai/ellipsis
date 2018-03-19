import * as React from 'react';
import Formatter from '../lib/formatter';
import ServerDataWarningNotificationData from "../models/notifications/server_data_warning_notification_data";
import autobind from '../lib/autobind';

interface Props {
  details: Array<ServerDataWarningNotificationData>
}

class NotificationForServerDataWarning extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

    getNetworkErrorMessage(networkError: ServerDataWarningNotificationData): string {
      const errorMessage = networkError.error && networkError.error.message ? networkError.error.message : "";
      if (/^401/.test(errorMessage)) {
        return "You have been signed out. Please reload the page.";
      } else if (/^404/.test(errorMessage)) {
        return "This skill may have been deleted on the server by someone else.";
      } else if (/^5\d\d/.test(errorMessage)) {
        return "The Ellipsis server responded with an error. You may need to reload the page.";
      } else {
        return "The Ellipsis server cannot be reached. Your connection may be down.";
      }
    }

    getNewerVersionMessage(notificationDetail: ServerDataWarningNotificationData): string {
      const versionData = notificationDetail.newerVersion;
      const name = versionData && versionData.author && versionData.author.formattedFullNameOrUserName();
      const timestamp = versionData ? Formatter.formatTimestampRelativeIfRecent(versionData.createdAt) : "recently";
      if (versionData && versionData.author && versionData.author.id === notificationDetail.currentUserId) {
        return `You saved a newer version of this skill in another window ${timestamp}.`;
      } else if (name) {
        return `${name} saved a newer version of this skill ${timestamp}.`;
      } else {
        return `Someone saved a newer version of this skill ${timestamp}.`;
      }
    }

    render() {
      const newerVersionDetail = this.props.details.find((detail) => detail.type === "newer_version");
      const networkError = this.props.details.find((detail) => detail.type === "network_error");
      let message = "";
      if (networkError) {
        message += this.getNetworkErrorMessage(networkError) + " ";
      }
      if (newerVersionDetail) {
        message += this.getNewerVersionMessage(newerVersionDetail);
      }

      return (
        <span>
          <span className="type-label">Warning: </span>
          <span className="mrs">{message}</span>
          {newerVersionDetail && newerVersionDetail.onClick && !networkError ? (
            <button className="button-s button-inverted" type="button" onClick={newerVersionDetail.onClick}>Reload the editor</button>
          ) : null}
        </span>
      );
    }
}

export default NotificationForServerDataWarning;
