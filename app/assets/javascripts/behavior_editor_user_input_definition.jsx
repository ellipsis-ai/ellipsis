define(function(require) {
var React = require('react'),
  BehaviorEditorDeleteButton = require('./behavior_editor_delete_button'),
  BehaviorEditorInput = require('./behavior_editor_input');

return React.createClass({
  displayName: 'BehaviorEditorUserInputDefinition',

  onNameChange: function(newName) {
    this.props.onChange({ name: newName, question: this.props.question });
  },

  onQuestionChange: function(newQuestion) {
    this.props.onChange({ name: this.props.name, question: newQuestion });
  },

  onDeleteClick: function() {
    this.props.onDelete();
  },

  focus: function() {
    this.refs.name.focus();
    this.refs.name.select();
  },

  render: function() {
    return (
      <div className={"columns " + (this.props.hasMargin ? "mts" : "")}>
        <div className="column column-one-quarter">
          <div className="columns columns-elastic">
            <div className="column column-expand prs">
              <BehaviorEditorInput
                ref="name"
                className="form-input-borderless type-monospace type-s"
                placeholder="userInput"
                value={this.props.name}
                onChange={this.onNameChange}
              />
            </div>
            <div className="column column-shrink align-b">
              <code className="type-s type-weak">,</code>
            </div>
          </div>
        </div>
        <div className="column column-three-quarters">
          <div className="columns columns-elastic">
            <div className="column column-expand prs">
              <div className="form-field-with-prefix">
                <label className="form-input-prefix"
                  htmlFor={"question" + this.props.id}
                  title="Write a question for @ellipsis to ask the user to provide this parameter."
                >Q:</label>
                <BehaviorEditorInput
                  id={"question" + this.props.id}
                  ref="question"
                  placeholder="Write a question to ask the user for this parameter"
                  autoFocus={this.props.shouldGrabFocus}
                  value={this.props.question}
                  onChange={this.onQuestionChange}
                  onEnterKey={this.props.onEnterKey}
                />
              </div>
            </div>
            <div className="column column-shrink">
              <BehaviorEditorDeleteButton
                onClick={this.onDeleteClick}
                title={"Delete the “" + this.props.name + "” parameter"}
              />
            </div>
          </div>
        </div>
      </div>
    );
  }
});

});
