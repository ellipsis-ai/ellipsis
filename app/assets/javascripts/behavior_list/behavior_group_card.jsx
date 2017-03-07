define(function(require) {
  var
    React = require('react'),
    BehaviorGroup = require('../models/behavior_group'),
    SVGInstall = require('../svg/install'),
    SVGInstalled = require('../svg/installed'),
    SVGInstalling = require('../svg/installing'),
    ifPresent = require('../lib/if_present');

  return React.createClass({
    displayName: 'BehaviorGroupCard',
    propTypes: {
      groupData: React.PropTypes.instanceOf(BehaviorGroup).isRequired,
      localId: React.PropTypes.string,
      description: React.PropTypes.string,
      name: React.PropTypes.string,
      icon: React.PropTypes.string,
      onBehaviorGroupImport: React.PropTypes.func,
      onMoreInfoClick: React.PropTypes.func.isRequired,
      isImportable: React.PropTypes.bool.isRequired,
      isImporting: React.PropTypes.bool,
      onSelectChange: React.PropTypes.func,
      isSelected: React.PropTypes.bool
    },

    isImportable: function() {
      return this.props.isImportable;
    },

    isImporting: function() {
      return this.props.isImporting;
    },

    isImported: function() {
      return !!this.props.localId;
    },

    importBehavior: function() {
      this.props.onBehaviorGroupImport(this.props.groupData);
    },

    toggleMoreInfo: function() {
      this.props.onMoreInfoClick(this.props.groupData);
    },

    getInstallButton: function() {
      if (!this.isImportable()) {
        return null;
      } else if (this.isImporting()) {
        return (
          <button title="Installing, please wait…" type="button" className="button-raw button-no-wrap" disabled="disabled" style={{ height: 24 }}>
            <span className="display-inline-block align-m mrs" style={{ width: 40, height: 24 }}><SVGInstalling /></span>
            <span className="display-inline-block align-m">
              Installing…
            </span>
          </button>
        );
      } else if (this.isImported()) {
        return (
          <button title="Already installed" type="button" className="button-raw button-no-wrap" disabled="disabled" style={{ height: 24 }}>
            <span className="display-inline-block align-m mrs" style={{ width: 40, height: 24 }}><SVGInstalled /></span>
            <span className="display-inline-block align-m type-green">
              Installed
            </span>
          </button>
        );
      } else {
        return (
          <button title="Install this skill" type="button" className="button-raw button-no-wrap" onClick={this.importBehavior} style={{ height: 24 }}>
            <span className="display-inline-block align-m mrs" style={{ width: 40, height: 24 }}><SVGInstall /></span>
            <span className="display-inline-block align-m">
              Install
            </span>
          </button>
        );
      }
    },

    getDescription: function() {
      return (
        <div style={{ maxHeight: "4rem", overflow: "hidden" }}>{this.props.description}</div>
      );
    },

    getMoreInfoText: function() {
      var actionCount = this.props.groupData.behaviorVersions.filter((ea) => !ea.isDataType()).length;
      if (actionCount === 0) {
        return "More info";
      } else if (actionCount === 1) {
        return "1 action";
      } else {
        return `${actionCount} actions`;
      }
    },

    renderDescription: function() {
      if (this.isImporting()) {
        return (
          <div>
            <div>{this.getDescription()}</div>
            <div className="type-disabled">More info</div>
          </div>
        );
      } else {
        return (
          <div>
            <div>{this.getDescription()}</div>
            <div>
              <button type="button" className="button-raw" onClick={this.toggleMoreInfo}>{this.getMoreInfoText()}</button>
              {this.isImported() ? (
                <span>
                  <span className="type-disabled phxs"> · </span>
                  <a href={jsRoutes.controllers.BehaviorEditorController.edit(this.props.localId).url}>Edit skill</a>
                </span>
                ) : null}
            </div>
          </div>
        );
      }
    },

    onSelectChange: function(event) {
      this.props.onSelectChange(this.props.localId, event.target.checked);
    },

    renderGroupSelectionCheckbox: function() {
      if (this.props.localId) {
        return (
          <input
            type="checkbox"
            onChange={this.onSelectChange}
            checked={this.props.isSelected}
            className="position-absolute position-top-left mtl mll"
          />
        );
      }
    },

    getName: function() {
      return this.props.name || (
        <span className="type-italic type-disabled">Untitled skill</span>
      );
    },

    render: function() {
      return (
        <div className="border border-radius bg-lightest plxxxl prl pvl position-relative">
          <div className={this.isImporting() ? "pulse" : ""}>
            {this.renderGroupSelectionCheckbox(this.props.localId)}
            <div className="type-l display-ellipsis mbm">
              {ifPresent(this.props.icon, (icon) => (
                <span style={{ width: "1em" }} className="display-inline-block mrm">
                  {icon}
                </span>
              ))}
              {this.getName()}
            </div>
            <div className="type-s mvm" style={{ height: "5.3333rem", overflow: "hidden" }}>
              {this.renderDescription()}
            </div>
            <div>
              {this.getInstallButton()}
            </div>
          </div>
        </div>
      );
    }
  });
});
