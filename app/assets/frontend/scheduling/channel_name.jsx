import * as React from 'react';
import ScheduleChannel from '../models/schedule_channel';

  class ChannelName extends React.PureComponent {
    render() {
      const channel = this.props.channel;
      return channel ? (
        <span>
          <span className="type-weak">{channel.getPrefix()} </span>
          <span>{channel.getName(this.props.slackUserId)} </span>
          <span className="type-weak">{channel.getSuffix()}</span>
        </span>
      ) : (
        <span className="type-disabled">
          {this.props.channelId ?
            `Channel ID ${this.props.channelId}` :
            "Unknown channel"
          }
        </span>
      );
    }
  }

  ChannelName.propTypes = {
    channel: React.PropTypes.instanceOf(ScheduleChannel),
    channelId: React.PropTypes.string,
    slackUserId: React.PropTypes.string
  };

export default ChannelName;
