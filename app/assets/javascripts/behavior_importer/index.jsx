define(function(require) {
  var React = require('react'),
    Group = require('./group');

  return React.createClass({
    propTypes: {
      teamId: React.PropTypes.string.isRequired,
      installedBehaviorGroups: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      csrfToken: React.PropTypes.string.isRequired
    },

    isImported: function(group) {
      return this.getInstalledBehaviorGroups().some(function(ea) {
        return ea.importedId === group.publishedId;
      });
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
        installedBehaviorGroups: this.getBehaviorGroups().concat([installedGroup])
      };
      this.setState(newState);
    },

    render: function() {
      return (
        <div>
          {this.getBehaviorGroups().map(function(group, index) {
            return (
              <Group
                key={"group" + index}
                csrfToken={this.props.csrfToken}
                name={group.name}
                description={group.description}
                groupData={group}
                teamId={this.props.teamId}
                behaviors={group.behaviorVersions}
                isImported={this.isImported(group)}
                onBehaviorGroupImport={this.onBehaviorGroupImport}
              />
            );
          }, this)}
        </div>
      );
    }
  });
});
