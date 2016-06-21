define(function(require) {
var React = require('react'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  BehaviorEditorDropdownMenu = require('./behavior_editor_dropdown_menu'),
  BehaviorEditorDropdownTrigger = require('./behavior_editor_dropdown_trigger');

return React.createClass({
  mixins: [BehaviorEditorMixin],
  propTypes: {
    onCancelClick: React.PropTypes.func.isRequired
  },
  getVersionText: function(versionIndex) {
    if (!this.props.versions || !this.props.versions[versionIndex]) {
      return "Loading…";
    } else if (versionIndex === 0) {
      return "Current version";
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
      selectedVersionIndex: 0,
      versionsMenuIsOpen: false
    };
  },
  getSelectedVersionIndex: function() {
    return this.state.selectedVersionIndex;
  },
  getVersionsMenu: function() {
    if (this.props.versions) {
      return this.props.versions.map(function(version, index) {
        return (
          <button key={"version" + index} type="button" className="button-invisible" onMouseUp={this.selectVersionIndex.bind(this, index)}>
            <span className={"mrxs " + this.visibleWhen(this.getSelectedVersionIndex() === index)}>✓</span>
            <span className={this.getSelectedVersionIndex() === index ? "type-bold" : ""}>{this.getVersionText(index)}</span>
          </button>
        );
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
    this.setState(this.getInitialState());
  },
  selectVersionIndex: function(index) {
    this.setState({ selectedVersionIndex: index });
  },
  toggleVersionsMenu: function() {
    this.setState({
      versionsMenuIsOpen: !this.state.versionsMenuIsOpen
    });
    this.refs.versionListTrigger.blur();
  },
  currentVersionSelected: function() {
    return this.getSelectedVersionIndex() === 0;
  },
  versionsMenuIsOpen: function() {
    return this.state.versionsMenuIsOpen
  },
  render: function() {
    return (
      <div className="box-action">
        <div className="container phn">
          <span className="align-button mrs">Version:</span>
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
          <button type="button" disabled={this.currentVersionSelected()} className="button-primary mrs">
            View
          </button>
          <button type="button" disabled={this.currentVersionSelected()} className="mrs">
            Restore
          </button>
          <button type="button" onClick={this.cancel}>Cancel</button>
        </div>
      </div>
    );
  }
});

});
