define(function(require) {
  var React = require('react'),
    AddNewBehaviorToGroup = require('./add_new_behavior_to_group'),
    BehaviorName = require('../behavior_list/behavior_name'),
    BehaviorVersion = require('../models/behavior_version'),
    ifPresent = require('../lib/if_present'),
    Sort = require('../lib/sort');

  return React.createClass({
    displayName: 'BehaviorSwitcherGroup',
    propTypes: {
      heading: React.PropTypes.string.isRequired,
      behaviors: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      currentBehavior: React.PropTypes.instanceOf(BehaviorVersion).isRequired,
      addNewUrl: React.PropTypes.string.isRequired,
      addNewLabel: React.PropTypes.string,
      emptyMessage: React.PropTypes.string.isRequired
    },

    getBehaviorList: function() {
      return Sort.arrayAlphabeticalBy(this.props.behaviors, (behavior) => behavior.sortKey);
    },

    isCurrentVersion: function(version) {
      return version.behaviorId === this.props.currentBehavior.behaviorId;
    },

    render: function() {
      return (
        <div className="mtxl mbxxl">
          <div className="container container-wide">
            <div className="columns columns-elastic">
              <div className="column column-expand">
                <h4>{this.props.heading}</h4>
              </div>
              <div className="column column-shrink">
                <AddNewBehaviorToGroup
                  url={this.props.addNewUrl}
                  label={this.props.addNewLabel}
                />
              </div>
            </div>
          </div>
          <div className="type-s">
            {ifPresent(this.getBehaviorList(), behaviors => behaviors.map((version) => (
              <div
                key={`behavior-${version.behaviorId}`}
                className={`border-top border-light pvxs container container-wide ${this.isCurrentVersion(version) ? "bg-blue border-blue-medium type-white" : ""}`}
              >
                <BehaviorName
                  version={version}
                  disableLink={this.isCurrentVersion(version)}
                  limitTriggers={true}
                  omitDescription={true}
                />
              </div>
            )), () => (
              <p className="container container-wide type-weak">{this.props.emptyMessage}</p>
            ))}
          </div>
        </div>
      );
    }
  });
});
