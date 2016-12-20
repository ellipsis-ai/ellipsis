define(function(require) {
  var React = require('react'),
    BehaviorGroupCard = require('./behavior_group_card');

  return React.createClass({
    propTypes: {
      teamId: React.PropTypes.string.isRequired,
      installedBehaviorGroups: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      csrfToken: React.PropTypes.string.isRequired
    },

    getLocalId: function(group) {
      const installed = this.getInstalledBehaviorGroups().find(ea => {
        return ea.importedId === group.publishedId;
      });
      return installed ? installed.groupId : null;
    },

    getInstalledBehaviorGroups: function() {
      return this.state.installedBehaviorGroups || [];
    },

    getBehaviorGroups: function() {
      return this.state.behaviorGroups || [];
    },

    getInitialState: function() {
      return {
        installedBehaviorGroups: this.props.installedBehaviorGroups,
        behaviorGroups: this.props.behaviorGroups
      };
    },

    onBehaviorGroupImport: function(installedGroup) {
      var newState = {
        installedBehaviorGroups: this.getInstalledBehaviorGroups().concat([installedGroup])
      };
      this.setState(newState);
    },

    render: function() {
      return (
        <div className="ptxxl">
          <div className="columns">
            {this.getBehaviorGroups().map((group, index) => (
              <div className="column column-one-third narrow-column-one-half mobile-column-full phl pbxxl mobile-phn" key={"group" + index}>
                <BehaviorGroupCard
                  csrfToken={this.props.csrfToken}
                  name={group.name}
                  description={group.description}
                  icon={group.icon}
                  groupData={group}
                  teamId={this.props.teamId}
                  localId={this.getLocalId(group)}
                  onBehaviorGroupImport={this.onBehaviorGroupImport}
                  onMoreInfoClick={function(){}}
                />
              </div>
            ))}
        </div>
        </div>
      );
    }
  });
});
