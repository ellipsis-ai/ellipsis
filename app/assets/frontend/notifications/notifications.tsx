import * as React from 'react';
import Notification from './notification';
import NotificationData from '../models/notification_data';
import NotificationDataGroup from '../models/notification_data_group';

interface Props {
  notifications: Array<NotificationData>,
  className?: string,
  inline?: Option<boolean>
}

interface State {
  notificationGroups: Array<NotificationDataGroup<any>>
}

class Notifications extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      notificationGroups: NotificationDataGroup.groupByKind(this.props.notifications)
    };
  }

    updateGroups(newNotifications: Array<NotificationData>): void {
      var newNotificationGroups = NotificationDataGroup.groupByKind(newNotifications);
      this.setState({
        notificationGroups: NotificationDataGroup.hideOldAndAppendNew(this.state.notificationGroups, newNotificationGroups)
      });
    }

    componentWillReceiveProps(newProps: Props) {
      this.updateGroups(newProps.notifications);
    }

    render() {
      return (
        <div className={this.props.className}>
          {this.state.notificationGroups.map((group, index) => (
            <Notification key={`notification-${group.kind}-${index}`} group={group} inline={this.props.inline} />
          ))}
        </div>
      );
    }
}

export default Notifications;
