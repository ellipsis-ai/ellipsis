define(function(require) {
var React = require('react'),
  Formatter = require('../lib/formatter'),
  BehaviorVersion = require('../models/behavior_version'),
  Select = require('../form/select');

return React.createClass({
  propTypes: {
    menuToggle: React.PropTypes.func.isRequired,
    onCancelClick: React.PropTypes.func.isRequired,
    onRestoreClick: React.PropTypes.func.isRequired,
    onSwitchVersions: React.PropTypes.func.isRequired,
    openMenuWhen: React.PropTypes.bool.isRequired,
    shouldFilterCurrentVersion: React.PropTypes.bool,
    versions: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired
  },
  getVersionText: function(versionIndex) {
    var text;
    if (versionIndex === 0 && this.props.versions.length === 1) {
      text = "Loading…";
    } else if (versionIndex === 0) {
      text = "Unsaved version";
    } else if (versionIndex === 1) {
      text = "Last saved version";
    } else {
      text = this.getDateForVersion(this.props.versions[versionIndex]);
    }
    return this.getVersionNumberForIndex(versionIndex) + text;
  },
  getDateForVersion: function(version) {
    return Formatter.formatTimestampRelativeIfRecent(version.createdAt);
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
  getVersionsMenu: function() {
    if (this.props.versions) {
      return this.props.versions.map(function(version, index) {
        if (index === 0 && this.props.shouldFilterCurrentVersion) {
          return null;
        } else {
          return (
            <option
              key={"version" + index}
              value={index}
            >{this.getVersionText(index)}</option>
          );
        }
      }, this);
    } else {
      return (
        <option value="">Loading…</option>
      );
    }
  },
  getVersionNumberForIndex: function(index) {
    return 'v' + (this.props.versions.length - index) + '. ';
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
              <h4 className="type-weak">Select a version to restore</h4>
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
