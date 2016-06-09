define(function(require) {
var React = require('react'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  BehaviorEditorDeleteButton = require('./behavior_editor_delete_button'),
  BehaviorEditorInput = require('./behavior_editor_input');

return React.createClass({
  displayName: 'BehaviorEditorTriggerInput',
  mixins: [BehaviorEditorMixin],
  isEmpty: function() {
    return !this.props.value;
  },

  focus: function() {
    this.refs.input.focus();
  },

  render: function() {
    return (
      <div className="columns columns-elastic">
        <div className="column column-expand">
          <BehaviorEditorInput
            className={"form-input-borderless " + (this.props.className || "")}
            ref="input"
            value={this.props.value}
            placeholder="Add a trigger phrase"
            onChange={this.props.onChange}
            onEnterKey={this.props.onEnterKey}
          />
        </div>
        <div className="column column-shrink">
          <div className="display-ellipsis">
            <div className="display-inline-block type-label">
              <label className="mrm">
                <input type="checkbox" /> Regexp
              </label>
              <label className="">
                <input type="checkbox" /> Case
              </label>
            </div>
            <div className={"display-inline-block" + this.visibleWhen(!this.props.hideDelete)}>
              <BehaviorEditorDeleteButton onClick={this.props.onDelete} />
            </div>
          </div>
        </div>
      </div>
    );
  }
});

});
