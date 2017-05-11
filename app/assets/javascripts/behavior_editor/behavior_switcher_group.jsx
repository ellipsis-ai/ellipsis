define(function(require) {
  var React = require('react'),
    AddNewBehaviorToGroup = require('./add_new_behavior_to_group'),
    BehaviorName = require('../behavior_list/behavior_name'),
    Editable = require('../models/editable'),
    LibraryName = require('./library_name'),
    ifPresent = require('../lib/if_present'),
    Sort = require('../lib/sort');

  return React.createClass({
    displayName: 'BehaviorSwitcherGroup',
    propTypes: {
      heading: React.PropTypes.string.isRequired,
      editables: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Editable)).isRequired,
      selectedId: React.PropTypes.string,
      onAddNew: React.PropTypes.func.isRequired,
      addNewLabel: React.PropTypes.string,
      emptyMessage: React.PropTypes.string.isRequired,
      onSelect: React.PropTypes.func.isRequired,
      isModified: React.PropTypes.func.isRequired
    },

    getSelected: function() {
      return this.props.editables.find(ea => this.props.selectedId && ((ea.libraryId === this.props.selectedId) || (ea.behaviorId === this.props.selectedId)) );
    },

    getEditableList: function() {
      return Sort.arrayAlphabeticalBy(this.props.editables, ea => ea.sortKey);
    },

    isSelected: function(editable) {
      return !!this.getSelected() && editable.id === this.getSelected().id;
    },

    renderNameFor: function(editable) {
      if (editable.isBehaviorVersion()) {
        return (
          <BehaviorName
            className="plxl mobile-pll"
            triggerClassName={this.isSelected(editable) ? "box-chat-selected" : "opacity-75"}
            version={editable}
            disableLink={this.isSelected(editable)}
            omitDescription={true}
            onClick={this.props.onSelect}
          />
        );
      } else {
        return (
          <LibraryName
            className="plxl mobile-pll"
            triggerClassName={this.isSelected(editable) ? "box-chat-selected" : "opacity-75"}
            version={editable}
            disableLink={this.isSelected(editable)}
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
            {ifPresent(this.getEditableList(), editables => editables.map((editable, index) => (
              <div
                key={`behavior${index}`}
                className={`pvxs ${this.isSelected(editable) ? "bg-blue border-blue-medium type-white" : ""}`}
              >
                <div className={"position-absolute position-left pls type-bold type-m " + (this.isSelected(editable) ? "" : "type-pink")}>
                  {this.props.isModified(editable) ? "•" : ""}
                </div>
                {this.renderNameFor(editable)}
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
