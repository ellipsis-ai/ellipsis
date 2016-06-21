define(function(require) {
var React = require('react'),
  BehaviorEditorDropdownMenu = require('./behavior_editor_dropdown_menu'),
  BehaviorEditorDropdownTrigger = require('./behavior_editor_dropdown_trigger');

return React.createClass({
  propTypes: {
    onCancelClick: React.PropTypes.func.isRequired
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
      return this.props.versions.map(function(version) {
        return (
          <button type="button" className="button-invisible" onMouseUp={function(){}}>
            {this.getDateForVersion(version)}
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
          <div>
            <BehaviorEditorDropdownTrigger
              ref="versionListTrigger"
              onClick={this.toggleVersionsMenu}
              openWhen={this.versionsMenuIsOpen()}
              className="button-dropdown-trigger-menu-above"
            >
              Current version
            </BehaviorEditorDropdownTrigger>
            <BehaviorEditorDropdownMenu
              isVisible={this.versionsMenuIsOpen()}
              onItemClick={this.toggleVersionsMenu}
              className="popup-dropdown-menu-above"
            >
              {this.getVersionsMenu()}
            </BehaviorEditorDropdownMenu>
          </div>
          <div className="mtl">
            <button type="button" className="button-primary mrs" onClick={this.props.onConfirmClick}>
              {this.props.confirmText || "OK"}
            </button>
            <button type="button" onClick={this.cancel}>Cancel</button>
          </div>
        </div>
      </div>
    );
  }
});

});
