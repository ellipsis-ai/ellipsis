define(function(require) {
var React = require('react'),
  BehaviorEditorDeleteButton = require('./behavior_editor_delete_button'),
  BehaviorEditorInput = require('./behavior_editor_input');

return React.createClass({
  displayName: 'BehaviorEditorTriggerInput',
  isEmpty: function() {
    return !this.props.value;
  },

  focus: function() {
    this.refs.input.focus();
  },

  render: function() {
    return (
      <div className="columns columns-elastic">
        <div className="column column-expand prn">
          <BehaviorEditorInput
            className="form-input-borderless"
            ref="input"
            value={this.props.value}
            placeholder="Add a trigger phrase or regular expression"
            onChange={this.props.onChange}
            onEnterKey={this.props.onEnterKey}
          />
        </div>
        <div className="column column-shrink">
          <BehaviorEditorDeleteButton onClick={this.props.onDelete} />
        </div>
      </div>
    );
  }
});

});
