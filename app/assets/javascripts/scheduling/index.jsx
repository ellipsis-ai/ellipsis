define(function(require) {
  var React = require('react'),
    Collapsible = require('../shared_ui/collapsible'),
    ScheduledAction = require('../models/scheduled_action'),
    ScheduledItem = require('./scheduled_item'),
    Sort = require('../lib/sort');

  return React.createClass({
    displayName: 'Scheduling',
    propTypes: {
      teamId: React.PropTypes.string.isRequired,
      scheduledActions: React.PropTypes.arrayOf(React.PropTypes.instanceOf(ScheduledAction)),
      teamTimeZone: React.PropTypes.string
    },

    getInitialState: function() {
      return {
        filterChannel: null
      };
    },

    getScheduleByChannel: function() {
      const groupsByName = {};
      this.props.scheduledActions.forEach((action) => {
        const channelName = action.channel || "Unknown";
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

    render: function() {
      const groups = this.getScheduleByChannel();
      return (
        <div>
          <div className="bg-light">
            <div className="container container-wide pbm">
              <h3 className="mvn ptxxl type-weak display-ellipsis">
                <span className="mrs">Scheduling</span>
              </h3>
            </div>
          </div>

          <div className="flex-columns">
            <div className="flex-column flex-column-left container container-wide phn">
              <div className="columns">
                <div className="column column-one-quarter ptxl phn">
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
                    {groups.map((group) => (
                      <button
                        className={`button-block width-full phxl mobile-phl pvxs mvxs ${
                            this.filterActiveFor(group.channelName) ? "bg-blue type-white " : "type-link "
                          }`}
                        key={`filter-${group.channelName}`}
                        onClick={() => this.toggleFilter(group.channelName)}
                      >
                        {group.channelName}
                      </button>
                    ))}
                  </div>
                </div>
                <div className="column column-three-quarters bg-white border-radius-bottom-left ptxl pbxxxxl">

                  {groups.map((group) => (
                    <Collapsible key={`group-${group.channelName}`} revealWhen={this.shouldShowChannel(group.channelName)}>
                      <div className="pvl">
                        <div className="phxxxl">
                          <h4>{group.channelName}</h4>
                        </div>

                        <div>
                          {group.actions.map((action, index) => (
                            <div className="pvxl phxxxl border-top" key={`action${index}`}>
                              <ScheduledItem scheduledAction={action} />
                            </div>
                          ))}
                        </div>
                      </div>
                    </Collapsible>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    }
  });
});
