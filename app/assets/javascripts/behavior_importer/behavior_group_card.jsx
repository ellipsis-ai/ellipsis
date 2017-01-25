define(function(require) {
  var
    React = require('react'),
    SVGInstall = require('../svg/install'),
    SVGInstalled = require('../svg/installed'),
    SVGInstalling = require('../svg/installing'),
    ifPresent = require('../lib/if_present');

  return React.createClass({
    displayName: 'BehaviorGroupCard',
    propTypes: {
      groupData: React.PropTypes.object.isRequired,
      localId: React.PropTypes.string,
      description: React.PropTypes.string,
      name: React.PropTypes.string.isRequired,
      icon: React.PropTypes.string,
      onBehaviorGroupImport: React.PropTypes.func.isRequired,
      onMoreInfoClick: React.PropTypes.func.isRequired,
      isImporting: React.PropTypes.bool
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
      if (this.isImporting()) {
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

    renderDescription: function() {
      if (this.isImporting()) {
        return (
          <div>
            {this.getDescription()}
            <div className="type-disabled">More info</div>
          </div>
        );
      } else if (this.isImported()) {
        return (
          <a href={jsRoutes.controllers.BehaviorEditorController.editGroup(this.props.localId).url}
            className="link-block">
            {this.getDescription()}
            <div className="link">View installed version</div>
          </a>
        );
      } else {
        return (
          <button type="button" className="button-block" onClick={this.toggleMoreInfo}>
            {this.getDescription()}
            <div className="link">More info</div>
          </button>
        );
      }
    },

    render: function() {
      return (
        <div className="border border-radius bg-lightest phxl pvl">
          <div className={this.isImporting() ? "pulse" : ""}>
            <div className="type-l display-ellipsis mbm">
              {ifPresent(this.props.icon, (icon) => (
                <span style={{ width: "1em" }} className="display-inline-block mrm">
                  {icon}
                </span>
              ))}
              {this.props.name}
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
