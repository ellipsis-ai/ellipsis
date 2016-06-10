define(function(require) {
var React = require('react'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  BehaviorEditorCheckbox = require('./behavior_editor_checkbox'),
  BehaviorEditorDeleteButton = require('./behavior_editor_delete_button'),
  BehaviorEditorInput = require('./behavior_editor_input');

return React.createClass({
  displayName: 'BehaviorEditorTriggerInput',
  mixins: [BehaviorEditorMixin],
  onChange: function(prop, newValue) {
    var newTrigger = {
      text: this.props.value,
      requiresMention: this.props.requiresMention,
      isRegex: this.props.isRegex,
      caseSensitive: this.props.caseSensitive
    };
    newTrigger[prop] = newValue;
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
      <div className="columns columns-elastic mbs">
        <div className={"column column-shrink prn " + (this.props.requiresMention ? "" : "display-none")}>
          <div className={"display-ellipsis type-weak type-s form-input form-input-borderless prxs " +
            (this.props.className || "")}>
            @ellipsis:
          </div>
        </div>
        <div className="column column-expand prn">
          <BehaviorEditorInput
            className={"form-input-borderless " + (this.props.className || "")}
            ref="input"
            value={this.props.value}
            placeholder="Add a trigger phrase"
            onChange={this.onChange.bind(this, 'text')}
            onEnterKey={this.props.onEnterKey}
          />
        </div>
        <div className="column column-shrink prn">
          <div className={"display-ellipsis form-input form-input-borderless " +
            (this.props.className || "")}>
            <label className="mrm type-label">
              <BehaviorEditorCheckbox
                checked={this.props.requiresMention}
                onChange={this.onChange.bind(this, 'requiresMention')}
              /> Mention
            </label>
            <label className="mrm type-label">
              <BehaviorEditorCheckbox
                checked={this.props.caseSensitive}
                onChange={this.onChange.bind(this, 'caseSensitive')}
              /> Case
            </label>
            <label className="type-label">
              <BehaviorEditorCheckbox
                checked={this.props.isRegex}
                onChange={this.onChange.bind(this, 'isRegex')}
              /> Regex
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
