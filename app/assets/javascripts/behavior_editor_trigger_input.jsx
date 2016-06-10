define(function(require) {
var React = require('react'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  BehaviorEditorCheckbox = require('./behavior_editor_checkbox'),
  BehaviorEditorDeleteButton = require('./behavior_editor_delete_button'),
  BehaviorEditorInput = require('./behavior_editor_input');

return React.createClass({
  displayName: 'BehaviorEditorTriggerInput',
  mixins: [BehaviorEditorMixin],
  changeTrigger: function(props) {
    var newTrigger = {
      text: this.props.value,
      requiresMention: this.props.requiresMention,
      isRegex: this.props.isRegex,
      caseSensitive: this.props.caseSensitive
    };
    Object.keys(props).forEach(function(key) {
      newTrigger[key] = props[key];
    });
    this.props.onChange(newTrigger);
  },
  onChange: function(propName, newValue) {
    var changes = {};
    changes[propName] = newValue;
    this.changeTrigger(changes);
  },
  validateText: function(newValue) {
    var text = newValue;
    var changes = {};
    if (this.props.isRegex && this.props.caseSensitive && text.indexOf("(?i)") === 0) {
      text = text.replace(/^\(\?i\)/, '');
      changes.caseSensitive = false;
      changes.text = text;
      this.changeTrigger(changes);
    }
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
          <div className={
            "type-weak type-s form-input form-input-borderless prxs " +
            (this.props.className || "")
          }>
            @ellipsis:
          </div>
        </div>
        <div className={"column column-shrink prn " + (this.props.isRegex ? "" : "display-none")}>
          <div className={"type-disabled type-monospace form-input form-input-borderless " + (this.props.className || "")}>/</div>
        </div>
        <div className="column column-expand prn">
          <BehaviorEditorInput
            className={
              " form-input-borderless " +
              (this.props.isRegex ? " type-monospace " : "") +
              (this.props.className || "")
            }
            ref="input"
            value={this.props.value}
            placeholder="Add a trigger phrase"
            onChange={this.onChange.bind(this, 'text')}
            onBlur={this.validateText}
            onEnterKey={this.props.onEnterKey}
          />
        </div>
        <div className={"column column-shrink prn " + (this.props.isRegex ? "" : "display-none")}>
          <div className={"type-disabled type-monospace form-input form-input-borderless prs " + (this.props.className || "")}>/</div>
        </div>
        <div className="column column-shrink prn">
          <div className={"display-ellipsis form-input form-input-borderless " +
            (this.props.className || "")}>
            <label className="mrm type-s" title="Only respond when someone mentions @ellipsis">
              <BehaviorEditorCheckbox
                checked={this.props.requiresMention}
                onChange={this.onChange.bind(this, 'requiresMention')}
              /> 🗣🤖
            </label>
            <label className="mrm type-s" title="Match uppercase and lowercase letters exactly — if unchecked, case is ignored">
              <BehaviorEditorCheckbox
                checked={this.props.caseSensitive}
                onChange={this.onChange.bind(this, 'caseSensitive')}
              /> <i>Aa</i>
            </label>
            <label className="type-s" title="Use regular expression pattern matching">
              <BehaviorEditorCheckbox
                checked={this.props.isRegex}
                onChange={this.onChange.bind(this, 'isRegex')}
              /> <code>/^…$/</code>
            </label>
          </div>
        </div>
        <div className="column column-shrink">
          <BehaviorEditorDeleteButton onClick={this.props.onDelete} hidden={this.props.hideDelete} />
        </div>
      </div>
    );
  }
});

});
