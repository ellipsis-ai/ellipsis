define(function(require) {
  var React = require('react'),
    Group = require('./group');

  return React.createClass({
    propTypes: {
      installedBehaviors: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      csrfToken: React.PropTypes.string.isRequired
    },

    behaviorIsImported: function(importId) {
      return this.getInstalledBehaviors().some(function(ea) {
        return ea.importedId === importId;
      });
    },

    getInstalledBehaviors: function() {
      return this.state.installedBehaviors || [];
    },

    getBehaviorGroups: function() {
      return this.state.behaviorGroups || [];
    },

    getInitialState: function() {
      var installed = this.props.installedBehaviors;
      return {
        installedBehaviors: installed,
        behaviorGroups: this.updateBehaviorGroupsWithLocalIds(this.props.behaviorGroups, function(publishedId) {
          var match = installed.find(function(ea) {
            return ea.importedId === publishedId;
          });
          return match ? match.behaviorId : null;
        })
      };
    },

    updateBehaviorGroupsWithLocalIds: function(groups, findMatchingLocalId) {
      return groups.map(function(group) {
        var versionsWithLocalIds = group.behaviorVersions.map(function(behaviorVersion) {
          var localId = findMatchingLocalId(behaviorVersion.config.publishedId);
          if (localId !== null) {
            return Object.assign({}, behaviorVersion, { localBehaviorId: localId });
          } else {
            return behaviorVersion;
          }
        }, this);
        return Object.assign({}, group, { behaviorVersions: versionsWithLocalIds });
      }, this);
    },

    onBehaviorImport: function(importId, localId) {
      var newState = {
        behaviorGroups: this.updateBehaviorGroupsWithLocalIds(this.getBehaviorGroups(), function(publishedId) {
          return publishedId === importId ? localId : null;
        })
      };
      if (!this.behaviorIsImported(importId)) {
        newState.installedBehaviors = this.state.installedBehaviors.concat({ behaviorId: localId, importedId: importId });
      }
      this.setState(newState);
    },

    render: function() {
      return (
        <div>
          <div className="bg-light">
            <div className="container ptxxxl pbm">
              <h2 className="type-weak mvn">Add behaviors to Ellipsis</h2>
            </div>
          </div>
          <div className="bg-white">
            <div className="container">
              {this.getBehaviorGroups().map(function(group, index) {
                return (
                  <Group
                    key={"group" + index}
                    csrfToken={this.props.csrfToken}
                    name={group.name}
                    description={group.description}
                    behaviors={group.behaviorVersions}
                    checkImported={this.behaviorIsImported}
                    onBehaviorImport={this.onBehaviorImport}
                  />
                );
              }, this)}
            </div>
          </div>
        </div>
      );
    }
  });
});
