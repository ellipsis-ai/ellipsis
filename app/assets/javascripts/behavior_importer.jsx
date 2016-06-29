define(function(require) {
  var React = require('react'),
    BehaviorImporterGroup = require('./behavior_importer_group');

  return React.createClass({
    propTypes: {
      alreadyImportedIds: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      csrfToken: React.PropTypes.string.isRequired
    },

    behaviorVersionIsInstalled: function(versionId) {
      return this.props.alreadyImportedIds.some(function(id) {
        return id === versionId;
      });
    },

    getBehaviorGroups: function() {
      return this.state.behaviorGroups || [];
    },

    getInitialState: function() {
      return {
        behaviorGroups: this.props.behaviorGroups
      };
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
                    checkInstalled={this.behaviorVersionIsInstalled}
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
