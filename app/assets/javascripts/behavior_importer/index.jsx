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

    behaviorGroupIsImported: function(importId) {
      return this.getInstalledBehaviorGroups().some(function(ea) {
        return ea.importedId === importId;
      });
    },

    getInstalledBehaviorGroups: function() {
      return this.state.installedBehaviorGroups || [];
    },

    getBehaviorGroups: function() {
      return this.state.behaviorGroups || [];
    },

    getInitialState: function() {
      var installed = this.props.installedBehaviorGroups;
      return {
        installedBehaviorGroups: installed,
        behaviorGroups: this.updateBehaviorGroupsWithLocalIds(this.props.behaviorGroups, function(publishedId) {
          var match = installed.find(function(ea) {
            return ea.importedId === publishedId;
          });
          return match ? match.groupId : null;
        })
      };
    },

    updateBehaviorGroupsWithLocalIds: function(groups, findMatchingLocalId) {
      return groups.map(function(group) {
        var localId = findMatchingLocalId(group.publishedId);
        return Object.assign({}, group, { localGroupId: localId });
      }, this);
    },

    onBehaviorGroupImport: function(importId, localId) {
      var newState = {
        behaviorGroups: this.updateBehaviorGroupsWithLocalIds(this.getBehaviorGroups(), function(publishedId) {
          return publishedId === importId ? localId : null;
        })
      };
      if (!this.behaviorGroupIsImported(importId)) {
        newState.installedBehaviorGroups = this.state.installedBehaviorGroups.concat({ behaviorId: localId, importedId: importId });
      }
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
                isImported={this.behaviorGroupIsImported(group)}
                onBehaviorGroupImport={this.onBehaviorGroupImport}
              />
            );
          }, this)}
        </div>
      );
    }
  });
});
