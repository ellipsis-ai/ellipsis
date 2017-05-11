define(function(require) {
  var React = require('react'),
    AddNewBehaviorToGroup = require('./add_new_behavior_to_group'),
    BehaviorName = require('../behavior_list/behavior_name'),
    LibraryName = require('./library_name'),
    BehaviorVersion = require('../models/behavior_version'),
    ifPresent = require('../lib/if_present'),
    Sort = require('../lib/sort');

  return React.createClass({
    displayName: 'BehaviorSwitcherGroup',
    propTypes: {
      heading: React.PropTypes.string.isRequired,
      behaviors: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      selectedId: React.PropTypes.string,
      onAddNew: React.PropTypes.func.isRequired,
      addNewLabel: React.PropTypes.string,
      emptyMessage: React.PropTypes.string.isRequired,
      onSelect: React.PropTypes.func.isRequired,
      isBehaviorModified: React.PropTypes.func.isRequired
    },

    getSelected: function() {
      return this.props.behaviors.find(ea => this.props.selectedId && ((ea.libraryId === this.props.selectedId) || (ea.behaviorId === this.props.selectedId)) );
    },

    getBehaviorList: function() {
      return Sort.arrayAlphabeticalBy(this.props.behaviors, (behavior) => behavior.sortKey);
    },

    isSelectedVersion: function(version) {
      return !!this.getSelected() && version.id === this.getSelected().id;
    },

    renderNameFor: function(version) {
      if (typeof version.isDataType === "function") {
        return (
          <BehaviorName
            className="plxl mobile-pll"
            triggerClassName={this.isSelectedVersion(version) ? "box-chat-selected" : "opacity-75"}
            version={version}
            disableLink={this.isSelectedVersion(version)}
            omitDescription={true}
            onClick={this.props.onSelect}
          />
        );
      } else {
        return (
          <LibraryName
            className="plxl mobile-pll"
            triggerClassName={this.isSelectedVersion(version) ? "box-chat-selected" : "opacity-75"}
            version={version}
            disableLink={this.isSelectedVersion(version)}
            omitDescription={true}
            onClick={this.props.onSelect}
          />
        );
      }
    },

    render: function() {
      return (
        <div className="border-bottom mtl pbl">
          <div className="container container-wide mbs">
            <h6>{this.props.heading}</h6>
          </div>
          <div className="type-s">
            {ifPresent(this.getBehaviorList(), behaviors => behaviors.map((version, index) => (
              <div
                key={`behavior${index}`}
                className={`pvxs ${this.isSelectedVersion(version) ? "bg-blue border-blue-medium type-white" : ""}`}
              >
                <div className={"position-absolute position-left pls type-bold type-m " + (this.isSelectedVersion(version) ? "" : "type-pink")}>
                  {this.props.isBehaviorModified(version) ? "•" : ""}
                </div>
                {this.renderNameFor(version)}
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
