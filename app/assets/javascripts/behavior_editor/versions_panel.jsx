define(function(require) {
var React = require('react'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  DropdownMenu = require('./dropdown_menu'),
  Formatter = require('../formatter');

return React.createClass({
  mixins: [BehaviorEditorMixin],
  propTypes: {
    menuToggle: React.PropTypes.func.isRequired,
    onCancelClick: React.PropTypes.func.isRequired,
    onRestoreClick: React.PropTypes.func.isRequired,
    onSwitchVersions: React.PropTypes.func.isRequired,
    openMenuWhen: React.PropTypes.bool.isRequired,
    shouldFilterCurrentVersion: React.PropTypes.bool,
    versions: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
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
            <DropdownMenu.Item
              key={"version" + index}
              onClick={this.selectVersionIndex.bind(this, index)}
              checkedWhen={this.getSelectedVersionIndex() === index}
              label={this.getVersionText(index)}
            />
          );
        }
      }, this);
    } else {
      return (
        <DropdownMenu.Item label="Loading…" />
      );
    }
  },
  getVersionNumberForIndex: function(index) {
    return (this.props.versions.length - index) + '. ';
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
      <div className="box-action">
        <div className="container phn">
          <div className="columns"><div className="column column-one-half">
          <button type="button" disabled={this.oldestVersionSelected()}
            className="button-symbol mrs"
            onClick={this.selectOldestVersion}
            title="Initial version"
          >|◄</button>
          <button type="button" disabled={this.oldestVersionSelected()}
            className="button-symbol mrs"
            onClick={this.incrementSelectedIndex}
            title="Previous version"
          >◄</button>
          <div className="display-inline-block position-relative">
            <DropdownMenu
              openWhen={this.props.openMenuWhen}
              label={this.getVersionText(this.getSelectedVersionIndex())}
              labelClassName="button-dropdown-trigger-menu-above button-dropdown-trigger-wide mrs"
              menuClassName="popup-dropdown-menu-above popup-dropdown-menu-wide"
              onDownArrow={this.incrementSelectedIndex}
              onUpArrow={this.decrementSelectedIndex}
              toggle={this.props.menuToggle}
            >
              {this.getVersionsMenu()}
            </DropdownMenu>
          </div>
          <button type="button" disabled={this.newestVersionSelected()}
            className="button-symbol mrs"
            onClick={this.decrementSelectedIndex}
            title="Next version"
          >►</button>
          <button type="button" disabled={this.newestVersionSelected()}
            className="button-symbol mrs"
            onClick={this.selectNewestVersion}
            title="Current version"
          >►|</button>
          </div><div className="column column-one-half align-r">

          <button type="button" disabled={this.currentVersionSelected()}
            className={"mrs " + (this.state.isRestoring ? "button-activated" : "")}
            onClick={this.restore}
          >
            <span className="button-labels">
              <span className="button-normal-label">Restore</span>
              <span className="button-activated-label">Restoring…</span>
            </span>
          </button>
          <button className="button-primary" type="button" onClick={this.cancel}>Cancel</button>
          </div></div>
        </div>
      </div>
    );
  }
});

});
