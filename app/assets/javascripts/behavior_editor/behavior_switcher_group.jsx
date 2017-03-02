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
      selectedBehavior: React.PropTypes.instanceOf(BehaviorVersion).isRequired,
      onAddNew: React.PropTypes.func.isRequired,
      addNewLabel: React.PropTypes.string,
      emptyMessage: React.PropTypes.string.isRequired,
      onSelectBehavior: React.PropTypes.func.isRequired,
      isBehaviorModified: React.PropTypes.func.isRequired
    },

    getBehaviorList: function() {
      return Sort.arrayAlphabeticalBy(this.props.behaviors, (behavior) => behavior.sortKey);
    },

    isSelectedVersion: function(version) {
      return version.behaviorId === this.props.selectedBehavior.behaviorId;
    },

    render: function() {
      return (
        <div className="border-bottom mtl pbl">
          <div className="container container-wide mbs">
            <h6>{this.props.heading}</h6>
          </div>
          <div className="type-s">
            {ifPresent(this.getBehaviorList(), behaviors => behaviors.map((version) => (
              <div
                key={`behavior-${version.behaviorId}`}
                className={`border-top border-light pvxs container container-wide ${this.isSelectedVersion(version) ? "bg-blue border-blue-medium type-white" : ""}`}
              >
                <div className={"position-absolute position-left pls type-bold type-m " + (this.isSelectedVersion(version) ? "" : "type-pink")}>
                  {this.props.isBehaviorModified(version) ? "•" : ""}
                </div>
                <BehaviorName
                  version={version}
                  disableLink={this.isSelectedVersion(version)}
                  limitTriggers={true}
                  omitDescription={true}
                  onClick={this.props.onSelectBehavior}
                />
              </div>
            )), () => (
              <p className="container container-wide type-weak">{this.props.emptyMessage}</p>
            ))}
          </div>
          <div className="container container-wide mvm">
            <AddNewBehaviorToGroup
              onClick={this.props.onAddNew}
              label={this.props.addNewLabel}
            />
          </div>
        </div>
      );
    }
  });
});
