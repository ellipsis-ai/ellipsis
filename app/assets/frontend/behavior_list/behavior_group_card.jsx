import * as React from 'react';
import BehaviorGroup from '../../javascripts/models/behavior_group';
import Checkbox from '../../javascripts/form/checkbox';
import SVGInstall from '../../javascripts/svg/install';
import SVGInstalled from '../../javascripts/svg/installed';
import SVGInstalling from '../../javascripts/svg/installing';

const BehaviorGroupCard = React.createClass({
    propTypes: {
      groupData: React.PropTypes.instanceOf(BehaviorGroup).isRequired,
      localId: React.PropTypes.string,
      description: React.PropTypes.node,
      name: React.PropTypes.node,
      icon: React.PropTypes.string,
      onBehaviorGroupImport: React.PropTypes.func,
      onMoreInfoClick: React.PropTypes.func.isRequired,
      isImportable: React.PropTypes.bool.isRequired,
      isImporting: React.PropTypes.bool,
      onCheckedChange: React.PropTypes.func,
      isChecked: React.PropTypes.bool,
      wasReimported: React.PropTypes.bool,
      cardClassName: React.PropTypes.string
    },

    isImportable: function() {
      return this.props.isImportable;
    },

    isImporting: function() {
      return this.props.isImporting;
    },

    isLocallyEditable: function() {
      return !!this.props.localId;
    },

    importBehavior: function() {
      this.props.onBehaviorGroupImport(this.props.groupData);
    },

    toggleMoreInfo: function() {
      this.props.onMoreInfoClick(this.props.groupData);
    },

    renderSecondaryAction: function() {
      if (!this.isImportable() && !this.isImporting() && this.isLocallyEditable()) {
        return (
          <Checkbox
            className="display-block type-s"
            onChange={this.onCheckedChange}
            checked={this.props.isChecked}
            label="Select"
          />
        );
      } else if (this.isImporting()) {
        return (
          <button title="Installing, please wait…" type="button" className="button-raw button-no-wrap" disabled="disabled" style={{ height: 24 }}>
            <span className="display-inline-block align-m mrs" style={{ width: 40, height: 24 }}><SVGInstalling /></span>
            <span className="display-inline-block align-m">
              Installing…
            </span>
          </button>
        );
      } else if (this.isLocallyEditable()) {
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

    renderWasReimported: function() {
      if (this.props.wasReimported) {
        return (
          <div className="type-s align-r fade-in">
            <span className="display-inline-block align-m mrs" style={{ width: 30, height: 18 }}><SVGInstalled /></span>
            <span className="display-inline-block align-m type-green">Re-installed</span>
          </div>
        );
      }
    },

    getDescription: function() {
      return (
        <div className="display-overflow-fade-bottom" style={{ maxHeight: "4rem" }}>
          {this.props.description}
        </div>
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

    onCheckedChange: function(isChecked) {
      this.props.onCheckedChange(this.props.localId, isChecked);
    },

    getName: function() {
      return this.props.name || (
        <span className="type-italic type-disabled">Untitled skill</span>
      );
    },

    renderIcon: function() {
      if (this.props.icon) {
        return (
          <span style={{ width: "1em" }} className="display-inline-block mrm type-icon">{this.props.icon}</span>
        );
      }
    },

    render: function() {
      return (
        <div className={"border border-radius position-relative " + (this.props.cardClassName || "")}>
          <div className={this.isImporting() ? "pulse" : ""}>
            <div className="phl pvm border-bottom border-light">
              <button type="button" className="button-block width-full" onClick={this.toggleMoreInfo} disabled={this.isImporting()}>
                <div className="type-l display-ellipsis mbm" style={{ height: "1.7778rem" }}>
                  {this.renderIcon()}
                  {this.getName()}
                </div>
                <div className="type-s display-overflow-hidden" style={{ height: "5.3334rem" }}>
                  <div>{this.getDescription()}</div>
                  <div>
                    <span className={this.isImporting() ? "type-disabled" : "link"}>{this.getMoreInfoText()}</span>
                  </div>
                </div>
              </button>
            </div>
            <div className="phl pvm width" style={{ height: "2.6667rem" }}>
              <div className="columns">
                <div className="column column-one-half">{this.renderSecondaryAction()}</div>
                <div className="column column-one-half">{this.renderWasReimported()}</div>
              </div>
            </div>
          </div>
        </div>
      );
    }
});

export default BehaviorGroupCard;
