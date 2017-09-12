define(function(require) {
var React = require('react'),
  Formatter = require('../lib/formatter'),
  BehaviorGroup = require('../models/behavior_group'),
  Select = require('../form/select');

return React.createClass({
  propTypes: {
    menuToggle: React.PropTypes.func.isRequired,
    onCancelClick: React.PropTypes.func.isRequired,
    onRestoreClick: React.PropTypes.func.isRequired,
    onSwitchVersions: React.PropTypes.func.isRequired,
    openMenuWhen: React.PropTypes.bool.isRequired,
    shouldFilterCurrentVersion: React.PropTypes.bool,
    versions: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)).isRequired
  },

  getInitialState: function() {
    return {
      isRestoring: false,
      selectedVersionIndex: null
    };
  },
  getSelectedVersionIndex: function() {
    if (this.state.selectedVersionIndex !== null) {
      return this.state.selectedVersionIndex;
    } else if (this.props.shouldFilterCurrentVersion) {
      return 1;
    } else {
      return 0;
    }
  },
  getAuthorForVersion: function(version) {
    return version.author ? version.author.formattedName() : "unknown";
  },
  getDateAndAuthorForVersion: function(version) {
    return `${Formatter.formatTimestampDate(version.createdAt)} by ${this.getAuthorForVersion(version)}`;
  },
  getGroupedVersions: function() {
    const groupedVersions = [];
    this.props.versions.reduce((prevDateAndAuthor, version, index) => {
      const versionDetail = {
        index: index,
        version: version
      };
      if (index === 0 && this.props.shouldFilterCurrentVersion) {
        return null;
      } else if (index === 0) {
        groupedVersions.push({
          label: "Unsaved",
          versions: [versionDetail]
        });
        return "Unsaved";
      } else {
        const dateAndAuthor = this.getDateAndAuthorForVersion(version);
        if (dateAndAuthor === prevDateAndAuthor) {
          groupedVersions[groupedVersions.length - 1].versions.push(versionDetail);
        } else {
          groupedVersions.push({
            label: "Saved on " + dateAndAuthor,
            versions: [versionDetail]
          });
        }
        return dateAndAuthor;
      }
    }, null);
    return groupedVersions;
  },
  getVersionOptionLabel: function(indexInGroup, versionIndex, version) {
    return `v${
      this.getVersionNumberForIndex(versionIndex)
    } ${
      indexInGroup === 0 ? Formatter.formatTimestampDate(version.createdAt) : ""
    } ${
      Formatter.formatTimestampTime(version.createdAt)
    } ${
      indexInGroup === 0 ? "by " + this.getAuthorForVersion(version) : ""
    } ${
      versionIndex === 1 ? "(last saved version)" : ""
    }`;
  },
  getVersionsMenu: function() {
    if (this.props.versions && this.props.versions.length > 1) {
      const groups = this.getGroupedVersions();
      return groups.map((group, groupIndex) => (
        <optgroup key={group.label} label={group.label}>
          {group.versions.map((versionDetail, versionIndexInGroup) => (
            <option key={`g${groupIndex}v${versionIndexInGroup}`} value={versionDetail.index}>{
              versionDetail.index === 0 ?
                "Current unsaved version" :
                this.getVersionOptionLabel(versionIndexInGroup, versionDetail.index, versionDetail.version)
            }</option>
          ))}
        </optgroup>
      ));
    } else {
      return (
        <option value="">Loading…</option>
      );
    }
  },
  getVersionNumberForIndex: function(index) {
    return this.props.versions.length - index;
  },
  cancel: function() {
    this.props.onCancelClick();
    this.reset();
  },
  reset: function() {
    this.setState(this.getInitialState());
  },
  restore: function() {
    this.setState({ isRestoring: true });
    this.props.onRestoreClick(this.getSelectedVersionIndex());
  },
  decrementSelectedIndex: function() {
    var selectedIndex = this.getSelectedVersionIndex();
    if ((selectedIndex - 1 >= 0 && !this.props.shouldFilterCurrentVersion) ||
        (selectedIndex -1 >= 1)) {
      this.selectVersionIndex(selectedIndex - 1);
    }
  },
  incrementSelectedIndex: function() {
    var selectedIndex = this.getSelectedVersionIndex();
    if (selectedIndex + 1 < this.props.versions.length) {
      this.selectVersionIndex(selectedIndex + 1);
    }
  },
  onSelectVersion: function(newValue) {
    var newIndex = parseInt(newValue, 10);
    if (!isNaN(newIndex)) {
      this.selectVersionIndex(newIndex);
    }
  },
  selectNewestVersion: function() {
    this.selectVersionIndex(this.props.shouldFilterCurrentVersion ? 1 : 0);
  },
  selectOldestVersion: function() {
    this.selectVersionIndex(this.props.versions.length - 1);
  },
  selectVersionIndex: function(index) {
    this.setState({ selectedVersionIndex: index });
    this.props.onSwitchVersions(index);
  },
  currentVersionSelected: function() {
    var selectedIndex = this.getSelectedVersionIndex();
    return selectedIndex === 0 || (selectedIndex === 1 && this.props.shouldFilterCurrentVersion);
  },
  newestVersionSelected: function() {
    var selectedIndex = this.getSelectedVersionIndex();
    return (
      (selectedIndex === 1 && this.props.shouldFilterCurrentVersion) ||
      (selectedIndex === 0)
    );
  },
  oldestVersionSelected: function() {
    return this.getSelectedVersionIndex() + 1 === this.props.versions.length;
  },
  render: function() {
    return (
      <div className="box-action phn">
        <div className="container">
          <div className="columns">
            <div className="column column-page-sidebar">
              <h4 className="mtn type-weak">Select a version to restore</h4>
            </div>
            <div className="column column-page-main">
              <div>
                <button type="button" disabled={this.oldestVersionSelected()}
                  className="mrs mbs"
                  onClick={this.selectOldestVersion}
                >|◄ Initial</button>
                <button type="button" disabled={this.oldestVersionSelected()}
                  className="mrs mbs"
                  onClick={this.incrementSelectedIndex}
                >◄ Previous</button>
                <button type="button" disabled={this.newestVersionSelected()}
                  className="mrs mbs"
                  onClick={this.decrementSelectedIndex}
                >Next ►</button>
                <button type="button" disabled={this.newestVersionSelected()}
                  className="mrs mbs"
                  onClick={this.selectNewestVersion}
                >Current ►|</button>
              </div>
              <div>
                <Select
                  className="width-30 mrs mbs"
                  onChange={this.onSelectVersion}
                  value={this.getSelectedVersionIndex()}
                >
                  {this.getVersionsMenu()}
                </Select>
              </div>
              <div className="ptxl">
                <button type="button" disabled={this.currentVersionSelected()}
                  className={"mrs mbs " + (this.state.isRestoring ? "button-activated" : "")}
                  onClick={this.restore}
                >
                  <span className="button-labels">
                    <span className="button-normal-label">Restore</span>
                    <span className="button-activated-label">Restoring…</span>
                  </span>
                </button>
                <button className="button-primary mbs" type="button" onClick={this.cancel}>Cancel</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }
});

});
