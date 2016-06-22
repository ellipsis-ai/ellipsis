define(function(require) {
var React = require('react'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  BehaviorEditorDropdownMenu = require('./behavior_editor_dropdown_menu');

return React.createClass({
  mixins: [BehaviorEditorMixin],
  propTypes: {
    onCancelClick: React.PropTypes.func.isRequired,
    onRestoreClick: React.PropTypes.func.isRequired,
    onSwitchVersions: React.PropTypes.func.isRequired
  },
  getVersionText: function(versionIndex) {
    if (versionIndex === 0 && this.props.versions.length === 1) {
      return "Loading…";
    } else if (versionIndex === 0) {
      return "Unsaved version";
    } else if (versionIndex === 1) {
      return "Last saved version";
    } else {
      return this.getDateForVersion(this.props.versions[versionIndex]);
    }
  },
  getDateForVersion: function(version) {
    var d = new Date(version.createdAt);
    // N.B. Safari doesn't support toLocaleString options at present
    return d.toLocaleString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: 'numeric',
      second: 'numeric',
      timeZoneName: 'short'
    });
  },
  getInitialState: function() {
    return {
      isRestoring: false,
      selectedVersionIndex: null,
      versionsMenuIsOpen: false
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
            <BehaviorEditorDropdownMenu.Item
              key={"version" + index}
              onClick={this.selectVersionIndex.bind(this, index)}
              checkedWhen={this.getSelectedVersionIndex() === index}
              label={this.getVersionText(index)}
            />
          );
        }
      }, this)
    } else {
      return (
        <BehaviorEditorDropdownMenu.Item label="Loading…" />
      );
    }
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
    this.props.onRestoreClick(this.getSelectedVersionIndex())
  },
  selectVersionIndex: function(index) {
    this.setState({ selectedVersionIndex: index });
    this.props.onSwitchVersions(index);
  },
  toggleVersionsMenu: function() {
    this.setState({
      versionsMenuIsOpen: !this.state.versionsMenuIsOpen
    });
  },
  currentVersionSelected: function() {
    var selectedIndex = this.getSelectedVersionIndex();
    return selectedIndex === 0 || (selectedIndex === 1 && this.props.shouldFilterCurrentVersion);
  },
  versionsMenuIsOpen: function() {
    return this.state.versionsMenuIsOpen
  },
  render: function() {
    return (
      <div className="box-action">
        <div className="container phn">
          <span className="align-button mrs">View version:</span>
          <div className="display-inline-block position-relative">
            <BehaviorEditorDropdownMenu
              openWhen={this.versionsMenuIsOpen()}
              label={this.getVersionText(this.getSelectedVersionIndex())}
              labelClassName="button-dropdown-trigger-menu-above button-dropdown-trigger-wide mrs"
              menuClassName="popup-dropdown-menu-above popup-dropdown-menu-wide"
              toggle={this.toggleVersionsMenu}
            >
              {this.getVersionsMenu()}
            </BehaviorEditorDropdownMenu>
          </div>
          <button type="button" disabled={this.currentVersionSelected()}
            className={"mrs " + (this.state.isRestoring ? "button-activated" : "")}
            onClick={this.restore}
          >
            <span className="button-labels">
              <span className="button-normal-label">Restore</span>
              <span className="button-activated-label">Restoring…</span>
            </span>
          </button>
          <button type="button" onClick={this.cancel}>Cancel</button>
        </div>
      </div>
    );
  }
});

});
