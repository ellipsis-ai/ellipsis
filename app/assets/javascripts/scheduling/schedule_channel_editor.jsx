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
        showChannels: false
      };
    },

    searchIncludes: function(channel) {
      return !this.state.searchText || channel.getName().toLowerCase().includes(this.state.searchText.toLowerCase());
    },

    getFilteredChannelList: function() {
      const channels = this.props.channelList.filter(
        (ea) => !ea.isArchived && ea.members.includes(this.props.slackUserId) && this.searchIncludes(ea)
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
      this.setState({
        searchText: newValue
      });
    },

    selectChannel: function(newValue) {
      this.props.onChange(newValue);
    },

    nameForChannel: function(channelId) {
      const channel = this.props.channelList.find((ea) => ea.id === channelId);
      return channel ? (
        <span>{channel.getFormattedName()}</span>
      ) : (
        <span className="type-disabled">None</span>
      );
    },

    toggleShowChannels: function() {
      this.setState({
        showChannels: !this.state.showChannels
      });
    },

    shouldShowChannels: function() {
      return this.props.scheduledAction.isNew() || this.state.showChannels;
    },

    showChannelButtonText: function() {
      return this.state.showChannels ? "Hide channels" : "Modify channel";
    },

    render: function() {
      const channelList = this.getFilteredChannelList();
      const hasNoMatches = Boolean(this.state.searchText) && channelList.length === 0;
      return (
        <div>
          <div className="type-s mbs">
            <span>Channel: </span>
            <b>{this.nameForChannel(this.props.scheduledAction.channel)}</b>
          </div>
          <Collapsible revealWhen={!this.props.scheduledAction.isNew()}>
            <div>
              <button type="button" className="button-s button-shrink" onClick={this.toggleShowChannels}>{this.showChannelButtonText()}</button>
            </div>
          </Collapsible>
          <Collapsible revealWhen={this.shouldShowChannels()}>
            <SearchWithResults
              placeholder="Search for a channel"
              value={this.props.scheduledAction.channel}
              options={channelList}
              isSearching={false}
              noMatches={hasNoMatches}
              onChangeSearch={this.updateSearch}
              onSelect={this.selectChannel}
            />
          </Collapsible>
        </div>
      );
    }
  });
});
