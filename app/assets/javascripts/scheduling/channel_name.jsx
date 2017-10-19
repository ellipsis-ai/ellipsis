define(function(require) {
  const React = require('react'),
    ScheduleChannel = require('../models/schedule_channel');

  class ChannelName extends React.PureComponent {
    render() {
      const channel = this.props.channel;
      return channel ? (
        <span>
          <span className="type-weak">{channel.getPrefix()} </span>
          <span>{channel.getName()} </span>
          <span className="type-weak">{channel.getSuffix()}</span>
        </span>
      ) : (
        <span className="type-disabled">Unknown</span>
      );
    }
  }

  ChannelName.propTypes = {
    channel: React.PropTypes.instanceOf(ScheduleChannel)
  };

  return ChannelName;
});
