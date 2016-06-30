define(function(require) {
  var React = require('react'),
    BehaviorImporterGroup = require('./behavior_importer_group');

  return React.createClass({
    propTypes: {
      alreadyImportedIds: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      csrfToken: React.PropTypes.string.isRequired
    },

    behaviorVersionIsImported: function(versionId) {
      return this.getAlreadyImportedIds().some(function(id) {
        return id === versionId;
      });
    },

    getAlreadyImportedIds: function() {
      return this.state.alreadyImportedIds || [];
    },

    getBehaviorGroups: function() {
      return this.state.behaviorGroups || [];
    },

    getInitialState: function() {
      return {
        alreadyImportedIds: this.props.alreadyImportedIds,
        behaviorGroups: this.props.behaviorGroups
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
      if (!this.behaviorVersionIsImported(importId)) {
        newState.alreadyImportedIds = this.state.alreadyImportedIds.concat(importId);
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
                    checkImported={this.behaviorVersionIsImported}
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
