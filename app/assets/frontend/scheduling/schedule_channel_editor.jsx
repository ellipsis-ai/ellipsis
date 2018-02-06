import * as React from 'react';
import Checkbox from '../../javascripts/form/checkbox';
import Collapsible from '../../javascripts/shared_ui/collapsible';
import SearchWithResults from '../../javascripts/form/search_with_results';
import SVGCheckmark from '../../javascripts/svg/checkmark';
import ChannelName from './channel_name';
import ScheduledAction from '../models/scheduled_action';
import ScheduleChannel from '../models/schedule_channel';

const ScheduleChannelEditor = React.createClass({
    propTypes: {
      scheduledAction: React.PropTypes.instanceOf(ScheduledAction).isRequired,
      channelList: React.PropTypes.arrayOf(React.PropTypes.instanceOf(ScheduleChannel)),
      slackUserId: React.PropTypes.string.isRequired,
      slackBotUserId: React.PropTypes.string.isRequired,
      onChange: React.PropTypes.func.isRequired
    },

    getInitialState: function() {
      return {
        searchText: "",
        showChannels: false
      };
    },

    hasChannelList: function() {
      return Boolean(this.props.channelList) && this.props.channelList.length > 0;
    },

    findChannelFor: function(channelId) {
      return this.hasChannelList() ? this.props.channelList.find((ea) => ea.id === channelId) : null;
    },

    componentWillUpdate: function(newProps) {
      if (newProps.scheduledAction.id !== this.props.scheduledAction.id) {
        this.setState(this.getInitialState());
      }
    },

    searchIncludes: function(channel, text) {
      if (!text) {
        return true;
      } else {
        const forComparison = text.replace(/^#/, "").toLowerCase();
        return channel.getName().toLowerCase().includes(forComparison);
      }
    },

    canSelectChannel: function(channel) {
      return !channel.isArchived && channel.members.includes(this.props.slackUserId);
    },

    getFilteredChannelList: function() {
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
    },

    updateSearch: function(newValue) {
      const selectedChannel = this.findChannelFor(this.props.scheduledAction.channel);
      if (!selectedChannel || !this.searchIncludes(selectedChannel, newValue)) {
        const newChannel = this.hasChannelList() ?
          this.props.channelList.find((ea) => this.canSelectChannel(ea) && this.searchIncludes(ea, newValue)) : null;
        this.selectChannel(newChannel ? newChannel.id : "");
      }
      this.setState({
        searchText: newValue
      });
    },

    selectChannel: function(newValue) {
      const useDM = this.channelIsDM(newValue) ? false : this.props.scheduledAction.useDM;
      this.props.onChange(newValue, useDM);
    },

    channelIsDM: function(channelId) {
      if (!channelId) {
        return false;
      }
      const selectedChannel = this.findChannelFor(channelId);
      return Boolean(selectedChannel && selectedChannel.isDM());
    },

    botMissingFromChannel: function() {
      const channelId = this.props.scheduledAction.channel;
      if (channelId && this.props.slackBotUserId) {
        const channelInfo = this.findChannelFor(channelId);
        if (channelInfo) {
          return !channelInfo.userCanAccess(this.props.slackBotUserId);
        }
      }
      return false;
    },

    updateDM: function(newValue) {
      this.props.onChange(this.props.scheduledAction.channel, newValue);
    },

    nameForChannel: function(channelId) {
      const foundChannel = channelId ? this.findChannelFor(channelId) : null;
      return (
        <ChannelName channel={foundChannel} channelId={channelId} />
      );
    },

    showChannels: function() {
      this.setState({
        showChannels: true
      }, () => {
        if (this.searcher) {
          this.searcher.clearSearch();
          this.searcher.focus();
        }
      });
    },

    shouldShowChannels: function() {
      return this.props.scheduledAction.isNew() || this.state.showChannels;
    },

    renderChannelWarning: function() {
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
    },

    render: function() {
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
});

export default ScheduleChannelEditor;
