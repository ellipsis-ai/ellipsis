import * as React from 'react';
import Notification from './notification';
import NotificationData from '../models/notification_data';
import NotificationDataGroup from '../models/notification_data_group';

const Notifications = React.createClass({
    propTypes: {
      notifications: React.PropTypes.arrayOf(React.PropTypes.instanceOf(NotificationData)).isRequired,
      className: React.PropTypes.string,
      inline: React.PropTypes.bool
    },

    getInitialState: function() {
      return {
        notificationGroups: NotificationDataGroup.groupByKind(this.props.notifications)
      };
    },

    updateGroups: function(newNotifications) {
      var newNotificationGroups = NotificationDataGroup.groupByKind(newNotifications);
      this.setState({
        notificationGroups: NotificationDataGroup.hideOldAndAppendNew(this.state.notificationGroups, newNotificationGroups)
      });
    },

    componentWillReceiveProps: function(newProps) {
      this.updateGroups(newProps.notifications);
    },

    render: function() {
      return (
        <div className={this.props.className}>
          {this.state.notificationGroups.map((group, index) => (
            <Notification key={`notification-${group.kind}-${index}`} group={group} inline={this.props.inline} />
          ))}
        </div>
      );
    }
});

export default Notifications;
