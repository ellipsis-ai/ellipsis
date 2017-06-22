define(function(require) {
  var React = require('react'),
    Checkbox = require('../form/checkbox'),
    Collapsible = require('../shared_ui/collapsible'),
    SearchWithResults = require('../form/search_with_results'),
    ScheduledAction = require('../models/scheduled_action'),
    ScheduleChannel = require('../models/schedule_channel');

  return React.createClass({
    displayName: 'ScheduleChannelEditor',
    propTypes: {
      scheduledAction: React.PropTypes.instanceOf(ScheduledAction).isRequired,
      channelList: React.PropTypes.arrayOf(React.PropTypes.instanceOf(ScheduleChannel)).isRequired,
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
      const channels = this.props.channelList.filter(
        (ea) => this.canSelectChannel(ea) && this.searchIncludes(ea, this.state.searchText)
      );
      const channelList = channels.map((ea) => ({
        name: ea.getFormattedName(),
        value: ea.id
      }));
      if (this.props.channelList.length > 0 && this.props.scheduledAction.channel) {
        return channelList;
      } else {
        return [{
          name: "No channel selected",
          value: ""
        }].concat(channelList);
      }
    },

    updateSearch: function(newValue) {
      const selectedChannel = this.props.channelList.find((ea) => ea.id === this.props.scheduledAction.channel);
      if (!selectedChannel || !this.searchIncludes(selectedChannel, newValue)) {
        const newChannel = this.props.channelList.find((ea) => this.canSelectChannel(ea) && this.searchIncludes(ea, newValue));
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
      const selectedChannel = this.props.channelList.find((ea) => ea.id === channelId);
      return Boolean(selectedChannel && selectedChannel.isDM());
    },

    botMissingFromChannel: function() {
      const channelId = this.props.scheduledAction.channel;
      if (channelId && this.props.slackBotUserId) {
        const channelInfo = this.props.channelList.find((ea) => ea.id === channelId);
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
      const channel = this.props.channelList.find((ea) => ea.id === channelId);
      return channel ? (
        <span>{channel.getFormattedName()}</span>
      ) : (
        <span className="type-disabled">None</span>
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
            — Warning: Ellipsis must be invited to this channel to run any action.
          </span>
        );
      } else if (this.props.scheduledAction.channel) {
        return (
          <span className="type-green">
            — Ellipsis can send messages in this channel.
          </span>
        );
      } else {
        return (
          <span className="type-pink">
            — Select a channel for Ellipsis to run this action.
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
          <div className="type-s mtm mbs">
            <span>Channel: </span>
            <b className="mrxs">{this.nameForChannel(this.props.scheduledAction.channel)}</b>
            {this.renderChannelWarning()}
          </div>
          <div className="type-s mvs">
            <Checkbox
              disabledWhen={isDM}
              checked={this.props.scheduledAction.useDM}
              onChange={this.updateDM}
              label="Send to each channel member privately"
              className={isDM ? "type-disabled" : ""}
            />
          </div>
          <Collapsible revealWhen={!this.shouldShowChannels()}>
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
});
