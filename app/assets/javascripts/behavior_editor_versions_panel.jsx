define(function(require) {
var React = require('react'),
  BehaviorEditorDropdownMenu = require('./behavior_editor_dropdown_menu'),
  BehaviorEditorDropdownTrigger = require('./behavior_editor_dropdown_trigger');

return React.createClass({
  propTypes: {
    onCancelClick: React.PropTypes.func.isRequired,
    onConfirmClick: React.PropTypes.func.isRequired
  },
  getInitialState: function() {
    return {
      versionsMenuIsOpen: false
    };
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
            >
              Current version
            </BehaviorEditorDropdownTrigger>
            <BehaviorEditorDropdownMenu
              isVisible={this.versionsMenuIsOpen()}
              onItemClick={this.toggleVersionsMenu}
            >
              <button type="button" className="button-invisible" onMouseUp={function(){}}>
                Current version
              </button>
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
