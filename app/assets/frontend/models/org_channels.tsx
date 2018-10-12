import ScheduleChannel, {ScheduleChannelJson} from "./schedule_channel";
import TeamChannels, {TeamChannelsJson} from "./team_channels";

export interface OrgChannelsJson {
  dmChannels: Array<ScheduleChannelJson>,
  mpimChannels: Array<ScheduleChannelJson>,
  orgSharedChannels: Array<ScheduleChannelJson>,
  externallySharedChannels: Array<ScheduleChannelJson>,
  teamChannels: Array<TeamChannelsJson>
}

export interface OrgChannelsInterface extends OrgChannelsJson {}

class OrgChannels implements OrgChannelsInterface {
  readonly dmChannels: Array<ScheduleChannel>;
  readonly mpimChannels: Array<ScheduleChannel>;
  readonly orgSharedChannels: Array<ScheduleChannel>;
  readonly externallySharedChannels: Array<ScheduleChannel>;
  readonly teamChannels: Array<TeamChannels>;

  allChannels(): Array<ScheduleChannel> {
    let channels: Array<ScheduleChannel> = [];
    this.teamChannels.forEach(ea => {
      channels = channels.concat(ea.channelList);
    });
    return channels.concat(this.orgSharedChannels).concat(this.dmChannels).concat(this.mpimChannels);
  }

  constructor(props: OrgChannelsInterface) {
    Object.defineProperties(this, {
      dmChannels: { value: props.dmChannels, enumerable: true },
      mpimChannels: { value: props.mpimChannels, enumerable: true },
      orgSharedChannels: { value: props.orgSharedChannels, enumerable: true },
      externallySharedChannels: { value: props.externallySharedChannels, enumerable: true },
      teamChannels: { value: props.teamChannels, enumerable: true }
    });
  }

  clone(props: Partial<OrgChannelsInterface>): OrgChannels {
    return new OrgChannels(Object.assign({}, this, props));
  }

  static fromJson(props: OrgChannelsJson): OrgChannels {
    return new OrgChannels(Object.assign({}, props, {
      dmChannels: props.dmChannels.map(ScheduleChannel.fromJson),
      mpimChannels: props.mpimChannels.map(ScheduleChannel.fromJson),
      orgSharedChannels: props.orgSharedChannels.map(ScheduleChannel.fromJson),
      externallySharedChannels: props.externallySharedChannels.map(ScheduleChannel.fromJson),
      teamChannels: props.teamChannels.map(TeamChannels.fromJson),
    }));
  }
}

export default OrgChannels;
