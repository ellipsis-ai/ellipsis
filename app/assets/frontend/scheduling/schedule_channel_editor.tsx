import * as React from 'react';
import Checkbox from '../form/checkbox';
import Collapsible from '../shared_ui/collapsible';
import SearchWithResults from '../form/search_with_results';
import SVGCheckmark from '../svg/checkmark';
import ChannelName from './channel_name';
import ScheduledAction from '../models/scheduled_action';
import ScheduleChannel from '../models/schedule_channel';
import autobind from "../lib/autobind";

interface Props {
  scheduledAction: ScheduledAction,
  channelList: Array<ScheduleChannel>,
  slackUserId: string,
  slackBotUserId: string,
  onChange: (channelId: string, useDM: boolean) => void
}

interface State {
  searchText: string,
  showChannels: boolean
}

interface ScheduleChannelOption {
  name: string,
  value: string
}

class ScheduleChannelEditor extends React.Component<Props, State> {
  searcher: Option<SearchWithResults>;

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
      return Boolean(this.props.channelList) && this.props.channelList.length > 0;
    }

    findChannelFor(channelId: string): Option<ScheduleChannel> {
      return this.hasChannelList() ? this.props.channelList.find((ea) => ea.id === channelId) : null;
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
      return !channel.isArchived && channel.userCanAccess(this.props.slackUserId);
    }

    getFilteredChannelList(): Array<ScheduleChannelOption> {
      const channels = this.hasChannelList() ? this.props.channelList.filter(
        (ea) => this.canSelectChannel(ea) && this.searchIncludes(ea, this.state.searchText)
      ) : [];
      const channelList = channels.map((ea) => ({
        name: ea.getFormattedName(),
        value: ea.id
      }));
      if (this.props.scheduledAction.channel) {
        if (this.hasChannelList()) {
          return channelList;
        } else {
          return [{
            name: `Channel ID ${this.props.scheduledAction.channel}`,
            value: this.props.scheduledAction.channel
          }];
        }
      } else {
        return [{
          name: "No channel selected",
          value: ""
        }].concat(channelList);
      }
    }

    updateSearch(newValue: string): void {
      const selectedChannel = this.findChannelFor(this.props.scheduledAction.channel);
      if (!selectedChannel || !this.searchIncludes(selectedChannel, newValue)) {
        const newChannel = this.hasChannelList() ?
          this.props.channelList.find((ea) => this.canSelectChannel(ea) && this.searchIncludes(ea, newValue)) : null;
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
      return Boolean(selectedChannel && selectedChannel.isDM());
    }

    botMissingFromChannel(): boolean {
      const channelId = this.props.scheduledAction.channel;
      if (channelId && this.props.slackBotUserId) {
        const channelInfo = this.findChannelFor(channelId);
        if (channelInfo) {
          return !channelInfo.userCanAccess(this.props.slackBotUserId);
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
      if (this.botMissingFromChannel()) {
        return (
          <span className="type-pink type-bold type-italic">
            Warning: Ellipsis must be invited to this channel to run any action.
          </span>
        );
      } else if (this.props.scheduledAction.channel) {
        return (
          <span className="type-green">
            <span className="display-inline-block height-l align-m mrs"><SVGCheckmark/></span>
            <span className="display-inline-block align-m">Ellipsis can send messages in this channel.</span>
          </span>
        );
      } else if (!this.hasChannelList()) {
        return (
          <span className="type-pink type-bold type-italic">
            Warning: An error occurred while trying to load the list of channels
          </span>
        );
      } else {
        return (
          <span className="type-pink">
            Select a channel for Ellipsis to run this action.
          </span>
        );
      }
    }

    render() {
      const channelList = this.getFilteredChannelList();
      const hasNoMatches = Boolean(this.state.searchText) && channelList.length === 0;
      const isDM = this.channelIsDM(this.props.scheduledAction.channel);
      return (
        <div>
          <Collapsible revealWhen={this.shouldShowChannels()}>
            <SearchWithResults
              ref={(searcher) => this.searcher = searcher}
              placeholder="Search for a channel"
              value={this.props.scheduledAction.channel || ""}
              options={channelList}
              isSearching={false}
              noMatches={hasNoMatches}
              onChangeSearch={this.updateSearch}
              onSelect={this.selectChannel}
              onEnterKey={this.selectChannel}
            />
          </Collapsible>
          <div className="mtm mbs">
            <div className="border border-light bg-white display-inline-block pvxs phs mbs">
              {this.nameForChannel(this.props.scheduledAction.channel)}
            </div>
            <div className="type-s">{this.renderChannelWarning()}</div>
          </div>
          <div className="type-s mvs">
            <Checkbox
              disabledWhen={isDM || !this.hasChannelList()}
              checked={this.props.scheduledAction.useDM}
              onChange={this.updateDM}
              label="Send to each channel member privately"
              className={isDM ? "type-disabled" : ""}
            />
          </div>
          <Collapsible revealWhen={!this.shouldShowChannels() && this.hasChannelList()}>
            <div>
              <button type="button" className="button-s" onClick={this.showChannels}>
                Change channel
              </button>
            </div>
          </Collapsible>
        </div>
      );
    }
}

export default ScheduleChannelEditor;
