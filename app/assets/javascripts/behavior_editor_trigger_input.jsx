define(function(require) {
var React = require('react'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  BehaviorEditorDeleteButton = require('./behavior_editor_delete_button'),
  BehaviorEditorInput = require('./behavior_editor_input');

return React.createClass({
  displayName: 'BehaviorEditorTriggerInput',
  mixins: [BehaviorEditorMixin],
  onChange: function(newValue) {
    var newTrigger = {
      text: newValue,
      requiresMention: false,
      isRegex: false,
      caseSensitive: false
    };
    this.props.onChange(newTrigger);
  },
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
            className={"form-input-borderless " + (this.props.className || "")}
            ref="input"
            value={this.props.value}
            placeholder="Add a trigger phrase"
            onChange={this.onChange}
            onEnterKey={this.props.onEnterKey}
          />
        </div>
        <div className={"column column-shrink" + this.visibleWhen(!this.props.hideDelete)}>
          <BehaviorEditorDeleteButton onClick={this.props.onDelete} />
        </div>
      </div>
    );
  }
});

});
