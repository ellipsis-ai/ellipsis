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
      <div className="columns columns-elastic mbs">
        <div className="column column-shrink align-m phs ptxs border-bottom bg-light border-radius-top">
          <div className="display-ellipsis type-s">
            <label>
              <input type="checkbox" /> @ellipsis:
            </label>
          </div>
        </div>
        <div className="column column-expand align-b prn">
          <BehaviorEditorInput
            className={"form-input-borderless phs " + (this.props.className || "")}
            ref="input"
            value={this.props.value}
            placeholder="Add a trigger phrase"
            onChange={this.props.onChange}
            onEnterKey={this.props.onEnterKey}
          />
        </div>
        <div className="column column-shrink align-m phs ptxs prxs border-bottom bg-light border-radius-top">
          <div className="display-ellipsis type-label">
            <label className="mrm">
              <input type="checkbox" /> Regexp
            </label>
            <label className="mrxs">
              <input type="checkbox" /> Case
            </label>
          </div>
        </div>
        <div className="column column-shrink align-b">
          <div className={this.visibleWhen(!this.props.hideDelete)}>
            <BehaviorEditorDeleteButton onClick={this.props.onDelete} />
          </div>
        </div>
      </div>
    );
  }
});

});
