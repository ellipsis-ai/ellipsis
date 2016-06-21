define(function(require) {
var React = require('react'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  BehaviorEditorDropdownMenu = require('./behavior_editor_dropdown_menu'),
  BehaviorEditorDropdownTrigger = require('./behavior_editor_dropdown_trigger');

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
            <button key={"version" + index} type="button" className="button-invisible" onMouseUp={this.selectVersionIndex.bind(this, index)}>
              <span className={"mrxs " + this.visibleWhen(this.getSelectedVersionIndex() === index)}>✓</span>
              <span className={this.getSelectedVersionIndex() === index ? "type-bold" : ""}>{this.getVersionText(index)}</span>
            </button>
          );
        }
      }, this)
    } else {
      return (
        <button type="button" className="button-invisible">
          Loading…
        </button>
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
    this.refs.versionListTrigger.blur();
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
            <BehaviorEditorDropdownTrigger
              ref="versionListTrigger"
              onClick={this.toggleVersionsMenu}
              openWhen={this.versionsMenuIsOpen()}
              className="button-dropdown-trigger-menu-above button-dropdown-trigger-wide mrs"
            >
              {this.getVersionText(this.getSelectedVersionIndex())}
            </BehaviorEditorDropdownTrigger>
            <BehaviorEditorDropdownMenu
              isVisible={this.versionsMenuIsOpen()}
              onItemClick={this.toggleVersionsMenu}
              className="popup-dropdown-menu-above popup-dropdown-menu-wide"
            >
              {this.getVersionsMenu()}
            </BehaviorEditorDropdownMenu>
          </div>
          <button type="button" disabled={this.currentVersionSelected()} className="mrs" onClick={this.restore}>
            Restore
          </button>
          <button type="button" onClick={this.cancel}>Cancel</button>
        </div>
      </div>
    );
  }
});

});
