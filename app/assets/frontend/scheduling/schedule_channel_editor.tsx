import * as React from 'react';
import Checkbox from '../form/checkbox';
import Collapsible from '../shared_ui/collapsible';
import SearchWithGroupedResults, {LabeledOptionGroup} from '../form/search_with_grouped_results';
import ChannelName from './channel_name';
import ScheduledAction from '../models/scheduled_action';
import ScheduleChannel from '../models/schedule_channel';
import autobind from "../lib/autobind";
import OrgChannels from "../models/org_channels";

interface Props {
  scheduledAction: ScheduledAction,
  orgChannels: OrgChannels,
  slackUserId: string,
  slackBotUserId: string,
  onChange: (channelId: string, useDM: boolean) => void
}

interface State {
  searchText: string,
  showChannels: boolean
}

interface ScheduleChannelGroup {
  groupName: string,
  channels: Array<ScheduleChannelOption>
}

interface ScheduleChannelOption {
  name: string,
  value: string
}

class ScheduleChannelEditor extends React.Component<Props, State> {
  searcher: Option<SearchWithGroupedResults>;

  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = this.defaultState();
  }

  defaultState(): State {
    return {
      searchText: "",
      showChannels: false
    };
  }

    hasChannelList(): boolean {
      return this.props.orgChannels.allChannels().length > 0;
    }

    findChannelFor(channelId: string): Option<ScheduleChannel> {
      return this.props.orgChannels.allChannels().find((ea) => ea.id === channelId);
    }

    componentWillUpdate(newProps: Props) {
      if (newProps.scheduledAction.id !== this.props.scheduledAction.id) {
        this.setState(this.defaultState());
      }
    }

    searchIncludes(channel: ScheduleChannel, text: string): boolean {
      if (!text) {
        return true;
      } else {
        const forComparison = text.replace(/^#/, "").toLowerCase();
        return channel.getName().toLowerCase().includes(forComparison);
      }
    }

    canSelectChannel(channel: ScheduleChannel): boolean {
      return !channel.isArchived && (!channel.isOtherDm || this.props.scheduledAction.channel === channel.id);
    }

  getFilteredChannelListFor(label: string, allChannels: Array<ScheduleChannel>): Array<ScheduleChannelOption> {
    const channels = allChannels.filter(
      (ea) => this.canSelectChannel(ea) && this.searchIncludes(ea, this.state.searchText)
    );
    const channelList = channels.map((ea) => ({
      name: ea.getFormattedName(),
      value: ea.id
    }));
    if (this.hasChannelList()) {
      return channelList;
    } else {
      return [{
        name: `Channel ID ${this.props.scheduledAction.channel}`,
        value: this.props.scheduledAction.channel
      }];
    }
  }
    getChannelGroupFor(label: string, channels: Array<ScheduleChannel>) {
      return {
        groupName: label,
        channels: this.getFilteredChannelListFor("Shared", channels)
      };
    }

    getFilteredChannelGroups(): Array<ScheduleChannelGroup> {
      let groups: Array<ScheduleChannelGroup> = [];
      groups.push(this.getChannelGroupFor("Direct messages", this.props.orgChannels.dmChannels));
      groups.push(this.getChannelGroupFor("Private groups", this.props.orgChannels.mpimChannels));
      this.props.orgChannels.teamChannels.forEach(ea => {
        groups.push(this.getChannelGroupFor(ea.teamName, ea.channelList));
      });
      groups.push(this.getChannelGroupFor("Shared within organization", this.props.orgChannels.orgSharedChannels));
      groups.push(this.getChannelGroupFor("Shared externally", this.props.orgChannels.externallySharedChannels));
      return groups;
    }

    updateSearch(newValue: string): void {
      const selectedChannel = this.findChannelFor(this.props.scheduledAction.channel);
      if (newValue && (!selectedChannel || !this.searchIncludes(selectedChannel, newValue))) {
        const newChannel = this.hasChannelList() ?
          this.props.orgChannels.allChannels().find((ea) => this.canSelectChannel(ea) && this.searchIncludes(ea, newValue)) : null;
        this.selectChannel(newChannel ? newChannel.id : "");
      }
      this.setState({
        searchText: newValue
      });
    }

    selectChannel(channelId: string): void {
      const useDM = this.channelIsDM(channelId) ? false : this.props.scheduledAction.useDM;
      this.props.onChange(channelId, useDM);
    }

    channelIsDM(channelId: string): boolean {
      if (!channelId) {
        return false;
      }
      const selectedChannel = this.findChannelFor(channelId);
      return Boolean(selectedChannel && selectedChannel.isDm());
    }

    channelMissing(): boolean {
      const channelId = this.props.scheduledAction.channel;
      return Boolean(channelId) && !this.findChannelFor(channelId);
    }

    channelArchived(): boolean {
      const channelId = this.props.scheduledAction.channel;
      const channel = this.findChannelFor(channelId);
      return Boolean(channel && channel.isArchived);
    }

    channelReadOnly(): boolean {
      const channelId = this.props.scheduledAction.channel;
      const channel = this.findChannelFor(channelId);
      return Boolean(channel && channel.isReadOnly);
    }

    botMissingFromChannel(): boolean {
      const channelId = this.props.scheduledAction.channel;
      if (channelId && this.props.slackBotUserId) {
        const channelInfo = this.findChannelFor(channelId);
        if (channelInfo) {
          return !channelInfo.isBotMember && !channelInfo.isSelfDm;
        }
      }
      return false;
    }

    updateDM(useDM: boolean): void {
      this.props.onChange(this.props.scheduledAction.channel, useDM);
    }

    nameForChannel(channelId: string) {
      const foundChannel = channelId ? this.findChannelFor(channelId) : null;
      return (
        <ChannelName channel={foundChannel} channelId={channelId} />
      );
    }

    showChannels(): void {
      this.setState({
        showChannels: true
      }, () => {
        if (this.searcher) {
          this.searcher.clearSearch();
          this.searcher.focus();
        }
      });
    }

    shouldShowChannels(): boolean {
      return this.props.scheduledAction.isNew() || this.state.showChannels;
    }

    renderChannelWarning() {
      if (this.channelMissing()) {
        return (
          <span className="type-pink type-bold type-italic">
            Warning: Unknown or deleted channel
          </span>
        );
      } else if (this.channelArchived()) {
        return (
          <span className="type-pink type-bold type-italic">
            Warning: This channel has been archived
          </span>
        );
      } else if (this.botMissingFromChannel()) {
        return (
          <span className="type-pink type-bold type-italic">
            Warning: The bot must be invited to this channel to run any action
          </span>
        );
      } else if (this.channelReadOnly()) {
        return (
          <span className="type-pink type-bold type-italic">
            Warning: The bot is restricted from posting to this channel by the admin
          </span>
        );
      } else if (!this.hasChannelList()) {
        return (
          <span className="type-pink type-bold type-italic">
            Warning: An error occurred while trying to load the list of channels
          </span>
        );
      } else {
        return null;
      }
    }

    render() {
      const channelOptionGroups: Array<LabeledOptionGroup> = this.getFilteredChannelGroups().map(ea => {
        return {
          label: ea.groupName,
          options: ea.channels
        }
      });
      const hasNoMatches = Boolean(this.state.searchText) && channelOptionGroups.every(ea => ea.options.length === 0);
      const isDM = this.channelIsDM(this.props.scheduledAction.channel);
      const shouldShowChannels = this.shouldShowChannels();
      const hasChannelList = this.hasChannelList();
      return (
        <div>
          <Collapsible revealWhen={shouldShowChannels}>
            <SearchWithGroupedResults
              ref={(searcher) => this.searcher = searcher}
              placeholder="Search for a channel"
              value={this.props.scheduledAction.channel || ""}
              optionGroups={channelOptionGroups}
              isSearching={false}
              noMatches={hasNoMatches}
              onChangeSearch={this.updateSearch}
              onSelect={this.selectChannel}
              onEnterKey={this.selectChannel}
              noValueOptionText={"No channel selected"}
            />
          </Collapsible>
          <div className="mtm mbs">
            <div className="border border-light bg-white display-inline-block align-m pvxs phs mbs mrm">
              {this.nameForChannel(this.props.scheduledAction.channel)}
            </div>
            <div className="display-inline-block align-m type-s mbs">{this.renderChannelWarning()}</div>
          </div>
          <div className="type-s mvs">
            <Checkbox
              disabledWhen={isDM || !hasChannelList}
              checked={this.props.scheduledAction.useDM}
              onChange={this.updateDM}
              label="Send to each channel member privately"
              className={isDM ? "type-disabled" : ""}
            />
          </div>
          <Collapsible revealWhen={!shouldShowChannels && hasChannelList}>
            {!shouldShowChannels && hasChannelList ? (
              <div>
                <button type="button" className="button-s" onClick={this.showChannels}>
                  Change channel
                </button>
              </div>
            ) : null}
          </Collapsible>
        </div>
      );
    }
}

export default ScheduleChannelEditor;
