define(function(require) {
  var React = require('react'),
    AddNewBehaviorToGroup = require('./add_new_behavior_to_group'),
    BehaviorName = require('../behavior_list/behavior_name'),
    BehaviorVersion = require('../models/behavior_version'),
    SVGXIcon = require('../svg/x'),
    Sort = require('../sort');

  return React.createClass({
    displayName: 'BehaviorSwitcher',
    propTypes: {
      behaviors: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      currentBehavior: React.PropTypes.instanceOf(BehaviorVersion).isRequired,
      onToggle: React.PropTypes.func.isRequired,
      groupId: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired
    },

    getBehaviorList: function() {
      return Sort.arrayAlphabeticalBy(this.props.behaviors.concat(this.props.currentBehavior), (behavior) => behavior.getFirstTriggerText());
    },

    isCurrentVersion: function(version) {
      return version.behaviorId === this.props.currentBehavior.behaviorId;
    },

    render: function() {
      return (
        <div className="bg-white width-20 position-relative ptl">
          <div className="phl">
            <div className="position-relative">
              <div className="position-absolute position-top-right">
                <button type="button" className="button-symbol button-s button-subtle" onClick={this.props.onToggle}><SVGXIcon /></button>
              </div>
              <h4 className="ptxs">{this.props.behaviors.length + 1} actions in this skill</h4>
              <div className="mvm">
                <AddNewBehaviorToGroup
                  groupId={this.props.groupId}
                  teamId={this.props.teamId}
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
