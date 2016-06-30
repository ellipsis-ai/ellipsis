define(function(require) {
  var React = require('react'),
    BehaviorImporterGroup = require('./behavior_importer_group');

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
        behaviorGroups: this.props.behaviorGroups.map(function(group) {
          var versionsWithLocalIds = group.behaviorVersions.map(function(behaviorVersion) {
            var match = installed.find(function(ea) {
              return ea.importedId == behaviorVersion.config.publishedId;
            });
            if ( match === undefined ) {
              return behaviorVersion;
            } else {
              return Object.assign({}, behaviorVersion, { localBehaviorId: match.behaviorId });
            }
          });
          return Object.assign({}, group, { behaviorVersions: versionsWithLocalIds });
        })
      };
    },

    onBehaviorImport: function(importId, localId) {
      var newState = {
        behaviorGroups: this.getBehaviorGroups().map(function(group) {
          return {
            name: group.name,
            description: group.description,
            behaviorVersions: group.behaviorVersions.map(function(behavior) {
              if (behavior.config.publishedId !== importId) {
                return behavior;
              }

              var newBehavior = {};
              Object.keys(behavior).forEach(function(key) {
                newBehavior[key] = behavior[key];
              });
              newBehavior.localBehaviorId = localId;
              return newBehavior;
            })
          };
        })
      };
      var installedBehavior = { behaviorId: localId, importedId: importId };
      if (!this.behaviorIsImported(importId)) {
        newState.installedBehaviors = this.state.installedBehaviors.concat(installedBehavior);
      }
      this.setState(newState);
    },

    render: function() {
      return (
        <div>
          <div className="bg-light">
            <div className="container ptxxxl pbm">
              <h2 className="type-weak mvn">Install behaviors from Ellipsis</h2>
            </div>
          </div>
          <div className="bg-white">
            <div className="container">
              {this.getBehaviorGroups().map(function(group, index) {
                return (
                  <BehaviorImporterGroup
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
