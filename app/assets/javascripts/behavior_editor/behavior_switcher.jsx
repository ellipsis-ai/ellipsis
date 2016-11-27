define(function(require) {
  var React = require('react'),
    AddNewBehaviorToGroup = require('./add_new_behavior_to_group'),
    BehaviorName = require('../behavior_list/behavior_name'),
    BehaviorVersion = require('../models/behavior_version'),
    Sort = require('../sort');

  return React.createClass({
    displayName: 'BehaviorSwitcher',
    propTypes: {
      heading: React.PropTypes.string.isRequired,
      behaviors: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      currentBehavior: React.PropTypes.instanceOf(BehaviorVersion).isRequired,
      addNewUrl: React.PropTypes.string.isRequired
    },

    getBehaviorList: function() {
      return Sort.arrayAlphabeticalBy(this.props.behaviors, (behavior) => behavior.getFirstTriggerText());
    },

    isCurrentVersion: function(version) {
      return version.behaviorId === this.props.currentBehavior.behaviorId;
    },

    render: function() {
      return (
        <div className="bg-white width-20 position-relative pbl">
          <div className="phl">
            <div className="position-relative">
              <h4 className="ptxs">{this.props.heading}</h4>
              <div className="mvm">
                <AddNewBehaviorToGroup
                  url={this.props.addNewUrl}
                />
              </div>
            </div>
          </div>
          <div className="type-s border-bottom">
            {this.getBehaviorList().map((version) => (
              <div
                key={`behavior-${version.behaviorId}`}
                className={`border-top border-bottom pvs phl mbneg1 ${this.isCurrentVersion(version) ? "bg-blue-lighter border-blue" : ""}`}
              >
                <BehaviorName version={version} disableLink={this.isCurrentVersion(version)} disableWrapping={true} />
              </div>
            ))}
          </div>
        </div>
      );
    }
  });
});
