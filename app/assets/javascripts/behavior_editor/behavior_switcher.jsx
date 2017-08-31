define(function(require) {
  var React = require('react'),
    BehaviorSwitcherGroup = require('./behavior_switcher_group'),
    BehaviorVersion = require('../models/behavior_version'),
    LibraryVersion = require('../models/library_version'),
    NodeModuleVersion = require('../models/node_module_version');

  return React.createClass({
    displayName: 'BehaviorSwitcher',
    propTypes: {
      actionBehaviors: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      dataTypeBehaviors: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      libraries: React.PropTypes.arrayOf(React.PropTypes.instanceOf(LibraryVersion)).isRequired,
      nodeModuleVersions: React.PropTypes.arrayOf(React.PropTypes.instanceOf(NodeModuleVersion)).isRequired,
      selectedId: React.PropTypes.string,
      groupId: React.PropTypes.string,
      groupName: React.PropTypes.string.isRequired,
      groupDescription: React.PropTypes.string.isRequired,
      onSelect: React.PropTypes.func.isRequired,
      addNewAction: React.PropTypes.func.isRequired,
      addNewDataType: React.PropTypes.func.isRequired,
      addNewLibrary: React.PropTypes.func.isRequired,
      isModified: React.PropTypes.func.isRequired,
      onUpdateNodeModules: React.PropTypes.func.isRequired
    },

    getSkillTitle: function() {
      return (
        <div className="mbxs">
          <h4 className="mbn">{this.props.groupName}</h4>
          <div className="type-s type-weak display-ellipsis display-limit-width"
            title={this.props.groupDescription}>
            {this.props.groupDescription}
          </div>
        </div>
      );
    },

    onEditSkillDetails: function() {
      this.props.onSelect(this.props.groupId, null);
    },

    getAllBehaviors: function() {
      return this.props.actionBehaviors.concat(this.props.dataTypeBehaviors);
    },

    getEditables: function() {
      return this.getAllBehaviors().concat(this.props.libraries);
    },

    getSelected: function() {
      return this.getEditables().find(ea => ea.getPersistentId() === this.props.selectedId);
    },

    renderNodeModules: function() {
      if (this.props.nodeModuleVersions.length) {
        return (
          <div className="border-bottom mtl pbl">
            <div className="container container-wide mbs">
              <h6>Required node modules</h6>
            </div>
            <div className="type-s">
              {this.props.nodeModuleVersions.map((version, index) => (
                <div
                  key={`nodeModuleVersion${index}`}
                  className={`pvxs`}
                >
                  <div className="plxl mobile-pll">{version.from} – v{version.version}</div>
                </div>
              ))}
            </div>
            <div className="container container-wide mvm">
              <button type="button"
                      onClick={this.props.onUpdateNodeModules}
                      className="button button-s button-shrink">Update node module versions</button>
            </div>
          </div>
        );
      } else {
        return null;
      }
    },

    render: function() {
      return (
        <div className="pbxxxl">

          <div className="border-bottom ptxl pbl mbl">

            <div className="container container-wide">
              {this.getSkillTitle()}
            </div>

            <div>
              <button type="button"
                className={
                  "button-block pvxs phxl mobile-phl width width-full " +
                  (this.getSelected() ? "" : "bg-blue border-blue-medium type-white")
                }
                onClick={this.onEditSkillDetails}
                disabled={!this.getSelected()}
              >
                <span className={`type-s ${this.getSelected() ? "link" : "type-white"}`}>Skill details</span>
              </button>
            </div>
          </div>

          {this.props.groupId ? (
            <div>
              <BehaviorSwitcherGroup
                ref="actionSwitcher"
                heading="Actions"
                editables={this.props.actionBehaviors}
                selectedId={this.props.selectedId}
                onAddNew={this.props.addNewAction}
                addNewLabel="Add new action"
                emptyMessage="Add actions to provide a response using custom data types for input."
                onSelect={this.props.onSelect}
                isModified={this.props.isModified}
              />

              <BehaviorSwitcherGroup
                ref="dataTypeSwitcher"
                heading="Data types"
                editables={this.props.dataTypeBehaviors}
                selectedId={this.props.selectedId}
                onAddNew={this.props.addNewDataType}
                addNewLabel="Add new data type"
                emptyMessage="Custom data types allow you to limit user input to a set of choices, backed by custom data."
                onSelect={this.props.onSelect}
                isModified={this.props.isModified}
              />

              <BehaviorSwitcherGroup
                ref="librarySwitcher"
                heading="Libraries"
                editables={this.props.libraries}
                selectedId={this.props.selectedId}
                onAddNew={this.props.addNewLibrary}
                addNewLabel="Add new library"
                emptyMessage="Libraries are shareable bits of code that you can require() from elsewhere in the skill"
                onSelect={this.props.onSelect}
                isModified={this.props.isModified}
              />

              {this.renderNodeModules()}
            </div>
          ) : null}

        </div>
      );
    }
  });
});
