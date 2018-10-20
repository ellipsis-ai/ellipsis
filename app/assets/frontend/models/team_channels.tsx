import ScheduleChannel, {ScheduleChannelJson} from "./schedule_channel";

export interface TeamChannelsJson {
  teamName: string,
  channelList: Array<ScheduleChannelJson>
}

export interface TeamChannelsInterface extends TeamChannelsJson {}

class TeamChannels implements TeamChannelsInterface {
  readonly teamName: string;
  readonly channelList: Array<ScheduleChannel>;

  constructor(props: TeamChannelsInterface) {
    Object.defineProperties(this, {
      teamName: { value: props.teamName, enumerable: true },
      channelList: { value: props.channelList, enumerable: true }
    });
  }

  clone(props: Partial<TeamChannelsInterface>): TeamChannels {
    return new TeamChannels(Object.assign({}, this, props));
  }

  static fromJson(props: TeamChannelsJson): TeamChannels {
    return new TeamChannels(Object.assign({}, props, { channelList: props.channelList.map(ScheduleChannel.fromJson) }));
  }
}

export default TeamChannels;
