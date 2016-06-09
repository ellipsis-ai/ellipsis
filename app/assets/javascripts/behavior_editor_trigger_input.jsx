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
        <div className="column column-shrink prn">
          <div className={"display-ellipsis type-s form-input form-input-borderless prxs " +
            (this.props.className || "")}>
            <label className="type-weak">
              @ellipsis:
            </label>
          </div>
        </div>
        <div className="column column-expand prn">
          <BehaviorEditorInput
            className={"form-input-borderless " + (this.props.className || "")}
            ref="input"
            value={this.props.value}
            placeholder="Add a trigger phrase"
            onChange={this.props.onChange}
            onEnterKey={this.props.onEnterKey}
          />
        </div>
        <div className="column column-shrink prn">
          <div className={"display-ellipsis form-input form-input-borderless " +
            (this.props.className || "")}>
            <label className="mrm type-label">
              <input type="checkbox" checked="checked" /> Mention?
            </label>
            <label className="mrm type-label">
              <input type="checkbox" /> Case?
            </label>
            <label className="type-label">
              <input type="checkbox" /> Regexp
            </label>
          </div>
        </div>
        <div className="column column-shrink">
          <div className={this.visibleWhen(!this.props.hideDelete)}>
            <BehaviorEditorDeleteButton onClick={this.props.onDelete} />
          </div>
        </div>
      </div>
    );
  }
});

});
