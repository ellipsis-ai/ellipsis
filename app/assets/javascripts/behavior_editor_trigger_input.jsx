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
        <div className="column column-expand align-b prn">
          <div className="form-field-capped">
            <div className="form-input-prefix align-m">
              <div className="type-s">
                <label>
                  <input type="checkbox" /> @ellipsis:
                </label>
              </div>
            </div>
            <BehaviorEditorInput
              className={this.props.className || ""}
              ref="input"
              value={this.props.value}
              placeholder="Add a trigger phrase"
              onChange={this.props.onChange}
              onEnterKey={this.props.onEnterKey}
            />
            <div className="form-input-suffix align-m">
              <div className="type-label">
                <label className="mrm">
                  <input type="checkbox" /> Regexp
                </label>
                <label className="mrxs">
                  <input type="checkbox" /> Case
                </label>
              </div>
            </div>
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
