define([
  'react',
  './behavior_editor_delete_button',
  './behavior_editor_input'
], function(
  React,
  BehaviorEditorDeleteButton,
  BehaviorEditorInput
) {

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
        <div className="column column-expand prs">
          <BehaviorEditorInput
            ref="input"
            value={this.props.value}
            placeholder="Add a trigger phrase or regular expression"
            onChange={this.props.onChange}
            onEnterKey={this.props.onEnterKey}
          />
        </div>
        <div className="column column-shrink">
          <BehaviorEditorDeleteButton
            onClick={this.props.onDelete}
            hidden={this.isEmpty() && this.props.mayHideDelete}
          />
        </div>
      </div>
    );
  }
});

});
