define(function(require) {
  var React = require('react'),
    ScheduledAction = require('../models/scheduled_action'),
    ScheduledItem = require('./scheduled_item');

  return React.createClass({
    displayName: 'Scheduling',
    propTypes: {
      teamId: React.PropTypes.string.isRequired,
      scheduledActions: React.PropTypes.arrayOf(React.PropTypes.instanceOf(ScheduledAction)),
      teamTimeZone: React.PropTypes.string
    },

    getScheduleByChannel: function() {
      const groupsByName = {};
      this.props.scheduledActions.forEach((action) => {
        const channelName = action.channel || "Unknown";
        if (groupsByName[channelName]) {
          groupsByName[channelName].push(action);
        } else {
          groupsByName[channelName] = [action];
        }
      });
      return Object.keys(groupsByName).map((channelName) => {
        return {
          channelName: channelName,
          actions: groupsByName[channelName]
        };
      });
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
            <div className="flex-column flex-column-left container container-wide prn">
              <div className="columns">
                <div className="column column-one-quarter ptxl">
                  <h5>Filter by channel:</h5>

                  {groups.map((group) => (
                    <div key={`filter-${group.channelName}`}>
                      <button type="button" className="button-raw">
                      {group.channelName}
                      </button>
                    </div>
                  ))}
                </div>
                <div className="column column-three-quarters bg-white border-radius-bottom-left ptxl pbxxxxl phxxxxl">

                  {groups.map((group) => (
                    <div className="mvxxl" key={`group-${group.channelName}`}>
                      <h4>{group.channelName}</h4>

                      <div>
                        {group.actions.map((action, index) => (
                          <div className="pvxl border-top" key={`action${index}`}>
                            <ScheduledItem scheduledAction={action} />
                          </div>
                        ))}
                      </div>
                    </div>
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
