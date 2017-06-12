define(function(require) {
  const React = require('react'),
    Collapsible = require('../shared_ui/collapsible'),
    FixedFooter = require('../shared_ui/fixed_footer'),
    ModalScrim = require('../shared_ui/modal_scrim'),
    PageWithPanels = require('../shared_ui/page_with_panels'),
    ScheduledAction = require('../models/scheduled_action'),
    ScheduleChannel = require('../models/schedule_channel'),
    ScheduledItem = require('./scheduled_item'),
    ScheduledItemEditor = require('./scheduled_item_editor'),
    Sort = require('../lib/sort');

  const Scheduling = React.createClass({
    displayName: 'Scheduling',
    propTypes: Object.assign(PageWithPanels.requiredPropTypes(), {
      teamId: React.PropTypes.string.isRequired,
      scheduledActions: React.PropTypes.arrayOf(React.PropTypes.instanceOf(ScheduledAction)),
      channelList: React.PropTypes.arrayOf(React.PropTypes.instanceOf(ScheduleChannel)),
      teamTimeZone: React.PropTypes.string,
      slackUserId: React.PropTypes.string
    }),

    getInitialState: function() {
      return {
        filterChannel: null,
        selectedItem: null
      };
    },

    getSelectedItem: function() {
      return this.state.selectedItem;
    },

    updateSelectedItem: function(newItem, optionalCallback) {
      this.setState({
        selectedItem: newItem
      }, optionalCallback);
    },

    getScheduleByChannel: function() {
      const groupsByName = {};
      this.props.scheduledActions.forEach((action) => {
        const channel = this.props.channelList.find((ea) => ea.id === action.channel);
        const channelName = channel ? channel.getFormattedName() : "Unknown";
        const group = groupsByName[channelName] || [];
        groupsByName[channelName] = group.concat([action]);
      });
      const channelNames = Object.keys(groupsByName);
      const sortedNames = Sort.arrayAlphabeticalBy(channelNames, (ea) => ea);
      return sortedNames.map((channelName) => {
        return {
          channelName: channelName,
          actions: groupsByName[channelName]
        };
      });
    },

    shouldShowChannel: function(channelName) {
      return !this.state.filterChannel || this.state.filterChannel === channelName;
    },

    toggleFilter: function(channelName) {
      const newState = {};
      if (this.filterActiveFor(channelName)) {
        newState.filterChannel = null;
      } else {
        newState.filterChannel = channelName;
      }
      this.setState(newState);
    },

    clearFilters: function() {
      this.setState({
        filterChannel: null
      });
    },

    filterActiveFor: function(channelName) {
      return this.state.filterChannel === channelName;
    },

    toggleEditor: function(action) {
      this.setState({
        selectedItem: action
      }, () => {
        this.props.onToggleActivePanel("moreInfo", true);
      });
    },

    addNewItem: function() {
      this.setState({
        selectedItem: ScheduledAction.newWithDefaults(this.props.teamTimeZone)
      }, () => {
        this.props.onToggleActivePanel("moreInfo", true);
      });
    },

    cancelEditor: function() {
      this.props.onClearActivePanel();
    },

    renderSidebar: function(groups) {
      return (
        <div>
          <div className="phxl mobile-phl mbs">
            <h5 className="display-inline-block prm">Filter by channel</h5>
            <span>
              <button type="button"
                className="button-s button-shrink"
                disabled={!this.state.filterChannel}
                onClick={this.clearFilters}
              >
                Clear
              </button>
            </span>
          </div>

          <div>
            {this.renderFilterList(groups)}
          </div>
        </div>
      );
    },

    renderFilterList: function(groups) {
      return groups.map((group) => (
        <button
          className={`button-block width-full phxl mobile-phl pvxs mvxs ${
            this.filterActiveFor(group.channelName) ? "bg-blue type-white " : "type-link "
            }`}
          key={`filter-${group.channelName}`}
          onClick={() => this.toggleFilter(group.channelName)}
        >
          {group.channelName}
        </button>
      ));
    },

    renderScheduleList: function(groups) {
      return groups.map((group) => (
        <Collapsible key={`group-${group.channelName}`} revealWhen={this.shouldShowChannel(group.channelName)}>
          <div className="pvl">
            <div className="phxxxl">
              <h4>{group.channelName}</h4>
            </div>

            <div>
              {group.actions.map((action) => (
                <div className="pvxl phxxxl border-top" key={`${action.type}-${action.id}`}>
                  <ScheduledItem scheduledAction={action} onClick={this.toggleEditor} />
                </div>
              ))}
            </div>
          </div>
        </Collapsible>
      ));
    },

    render: function() {
      const groups = this.getScheduleByChannel();
      const selectedItem = this.getSelectedItem();
      return (
        <div>
          <div className="bg-light">
            <div className="container container-wide pvxl">
              <div className="columns columns-elastic mobile-columns-float">
                <div className="column column-expand align-b">
                  <h3 className="mvn type-weak display-ellipsis">
                    <span className="mrs">Scheduling</span>
                  </h3>
                </div>
                <div className="column column-shrink">
                  <button type="button" className="button-shrink" onClick={this.addNewItem}>Schedule something new</button>
                </div>
              </div>
            </div>
          </div>

          <div className="flex-columns">
            <div className="flex-column flex-column-left container container-wide phn">
              <div className="columns">
                <div className="column column-one-quarter ptxl phn">
                  {this.renderSidebar(groups)}
                </div>
                <div className="column column-three-quarters bg-white border-radius-bottom-left ptxl pbxxxxl">
                  {this.renderScheduleList(groups)}
                </div>
              </div>
            </div>
          </div>

          <ModalScrim isActive={this.props.activePanelIsModal} onClick={this.props.onClearActivePanel} />
          <FixedFooter ref="footer" className="bg-white">
            <Collapsible
              ref="moreInfo"
              revealWhen={this.props.activePanelName === 'moreInfo'}
            >
              <ScheduledItemEditor
                scheduledAction={selectedItem}
                channelList={this.props.channelList}
                onChange={this.updateSelectedItem}
                onCancel={this.cancelEditor}
                teamTimeZone={this.props.teamTimeZone || "America/New_York"}
                slackUserId={this.props.slackUserId || ""}
              />
            </Collapsible>
          </FixedFooter>
        </div>
      );
    }
  });

  return PageWithPanels.with(Scheduling);
});
