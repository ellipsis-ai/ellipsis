define(function(require) {
  var React = require('react'),
    Notification = require('./notification'),
    NotificationData = require('../models/notification_data'),
    NotificationDataGroup = require('../models/notification_data_group');

  return React.createClass({
    displayName: 'Notifications',
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
      if (this.state.notificationGroups.length > 0) {
        return (
          <div className={this.props.className}>
            {this.state.notificationGroups.map((group, index) => (
              <Notification key={"notification" + index} group={group} inline={this.props.inline} />
            ))}
          </div>
        );
      } else {
        return null;
      }
    }
  });
});
