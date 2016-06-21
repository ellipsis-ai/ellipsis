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
  getCurrentVersion: function() {
    if (this.props.versions && this.props.versions[0]) {
      return this.getDateForVersion(this.props.versions[0]);
    } else {
      return "Loading…";
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
      versionsMenuIsOpen: false
    };
  },
  getVersionsMenu: function() {
    if (this.props.versions) {
      return this.props.versions.map(function(version, index) {
        return (
          <button key={"version" + index} type="button" className="button-invisible" onMouseUp={function(){}}>
            <span className={"mrxs " + this.visibleWhen(index === 0)}>✓</span>
            <span className={index === 0 ? "type-bold" : ""}>{this.getDateForVersion(version)}</span>
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
  toggleVersionsMenu: function() {
    this.setState({
      versionsMenuIsOpen: !this.state.versionsMenuIsOpen
    });
    this.refs.versionListTrigger.blur();
  },
  versionsMenuIsOpen: function() {
    return this.state.versionsMenuIsOpen
  },
  render: function() {
    return (
      <div className="box-action">
        <div className="container phn">
          <BehaviorEditorDropdownTrigger
            ref="versionListTrigger"
            onClick={this.toggleVersionsMenu}
            openWhen={this.versionsMenuIsOpen()}
            className="button-dropdown-trigger-menu-above button-dropdown-trigger-wide mrs"
          >
            {this.getCurrentVersion()}
          </BehaviorEditorDropdownTrigger>
          <button type="button" className="button-primary mrs">View version</button>
          <button type="button" className="mrs">Restore version</button>
          <button type="button" onClick={this.cancel}>Cancel</button>
          <BehaviorEditorDropdownMenu
            isVisible={this.versionsMenuIsOpen()}
            onItemClick={this.toggleVersionsMenu}
            className="popup-dropdown-menu-above popup-dropdown-menu-wide"
          >
            {this.getVersionsMenu()}
          </BehaviorEditorDropdownMenu>
        </div>
      </div>
    );
  }
});

});
