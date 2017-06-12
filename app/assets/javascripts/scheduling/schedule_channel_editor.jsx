define(function(require) {
  var React = require('react'),
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
      onChange: React.PropTypes.func.isRequired
    },

    getInitialState: function() {
      return {
        searchText: "",
        selectedValue: this.props.scheduledAction.channel,
        showChannels: false
      };
    },

    componentWillUpdate: function(newProps) {
      if (newProps.scheduledAction.channel !== this.props.scheduledAction.channel) {
        this.setState({
          searchText: "",
          selectedValue: newProps.scheduledAction.channel
        });
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
      if (this.props.scheduledAction.isNew()) {
        // Provide a blank option if there might not be a channel set
        return [{
          name: "(none)",
          value: ""
        }].concat(channelList);
      } else {
        return channelList;
      }
    },

    updateSearch: function(newValue) {
      const newState = {
        searchText: newValue
      };
      const selectedChannel = this.props.channelList.find((ea) => ea.id === this.state.selectedValue);
      if (!selectedChannel || !this.searchIncludes(selectedChannel, newValue)) {
        const newChannel = this.props.channelList.find((ea) => this.canSelectChannel(ea) && this.searchIncludes(ea, newValue));
        newState.selectedValue = newChannel ? newChannel.id : "";
      }
      this.setState(newState);
    },

    selectChannel: function(newValue) {
      this.setState({
        selectedValue: newValue
      });
    },

    updateChannel: function() {
      if (this.state.selectedValue) {
        this.setState({
          showChannels: false
        }, () => {
          this.props.onChange(this.state.selectedValue);
        });
      }
    },

    undoChannel: function() {
      this.setState({
        selectedValue: this.props.scheduledAction.channel,
        showChannels: false
      });
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

    render: function() {
      const channelList = this.getFilteredChannelList();
      const hasNoMatches = Boolean(this.state.searchText) && channelList.length === 0;
      return (
        <div>
          <Collapsible revealWhen={this.shouldShowChannels()}>
            <SearchWithResults
              ref={(searcher) => this.searcher = searcher}
              placeholder="Search for a channel"
              value={this.state.selectedValue}
              options={channelList}
              isSearching={false}
              noMatches={hasNoMatches}
              onChangeSearch={this.updateSearch}
              onSelect={this.selectChannel}
              onEnterKey={this.updateChannel}
            />
            <button type="button"
              className="button-s button-shrink mrs"
              disabled={!this.state.selectedValue}
              onClick={this.updateChannel}>Select channel</button>
            {this.props.scheduledAction.isNew() ? null : (
              <button type="button"
                className="button-s button-shrink mrs"
                onClick={this.undoChannel}>Cancel</button>
            )}
          </Collapsible>
          <div className="type-s mtm mbs">
            <span>Channel: </span>
            <b>{this.nameForChannel(this.props.scheduledAction.channel)}</b>
          </div>
          <Collapsible revealWhen={!this.shouldShowChannels()}>
            <div>
              <button type="button" className="button-s button-shrink" onClick={this.showChannels}>Modify channel</button>
            </div>
          </Collapsible>
        </div>
      );
    }
  });
});
