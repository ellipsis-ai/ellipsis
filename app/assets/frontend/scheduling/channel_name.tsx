import * as React from 'react';
import ScheduleChannel from '../models/schedule_channel';

interface Props {
  channel?: Option<ScheduleChannel>
  channelId?: Option<string>
}

  class ChannelName extends React.PureComponent<Props> {
    render() {
      const channel = this.props.channel;
      return channel ? (
        <span>
          <span className="type-weak">{channel.getPrefix()} </span>
          <span>{channel.getName()} </span>
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

export default ChannelName;
