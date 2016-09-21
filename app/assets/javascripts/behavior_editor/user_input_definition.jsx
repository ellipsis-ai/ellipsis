define(function(require) {
var React = require('react'),
  DeleteButton = require('./delete_button'),
  Input = require('../form/input');

  var paramTypeDescriptions = {
    "Text": "Some text",
    "Number": "A number"
  };

return React.createClass({
  propTypes: {
    id: React.PropTypes.oneOfType([
      React.PropTypes.number,
      React.PropTypes.string
    ]).isRequired,
    name: React.PropTypes.string.isRequired,
    paramTypes: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
    paramType: React.PropTypes.shape({
      name: React.PropTypes.string
    }).isRequired,
    onChange: React.PropTypes.func.isRequired,
    onDelete: React.PropTypes.func.isRequired,
    onEnterKey: React.PropTypes.func.isRequired,
    onNameFocus: React.PropTypes.func.isRequired,
    onNameBlur: React.PropTypes.func.isRequired,
    question: React.PropTypes.string.isRequired,
    shouldGrabFocus: React.PropTypes.bool
  },

  onNameChange: function(newName) {
    this.props.onChange({ name: newName, paramType: this.props.paramType, question: this.props.question });
  },

  onParamTypeChange: function(event) {
    var newTypeName = event.target.value;
    this.props.onChange({ name: this.props.name, paramType: { name: newTypeName }, question: this.props.question });
  },

  onQuestionChange: function(newQuestion) {
    this.props.onChange({ name: this.props.name, paramType: this.props.paramType, question: newQuestion });
  },

  onDeleteClick: function() {
    this.props.onDelete();
  },

  focus: function() {
    this.refs.name.focus();
    this.refs.name.select();
  },

  keyFor: function(paramTypeName) {
    return 'param-type-' + this.props.id + '-' + paramTypeName;
  },

  paramTypeDisplayNameFor: function(paramTypeName) {
    return paramTypeDescriptions[paramTypeName] || paramTypeName;
  },

  render: function() {
    return (
      <div>
        <div className="columns columns-elastic">
          <div className="column column-expand align-form-input">
            <select className="form-select form-select-s min-width-10 align-m mrm" name="paramType" value={this.props.paramType.name} onChange={this.onParamTypeChange}>
              {this.props.paramTypes.map((paramTypeName) => (
                <option value={paramTypeName} key={this.keyFor(paramTypeName)}>
                  {this.paramTypeDisplayNameFor(paramTypeName)}
                </option>
              ))}
            </select>
            <span className="display-inline-block align-m type-s type-weak mrm">labeled</span>
            <Input
              ref="name"
              className="form-input-borderless type-monospace type-s width-10 mrm"
              placeholder="userInput"
              value={this.props.name}
              onChange={this.onNameChange}
              onFocus={this.props.onNameFocus}
              onBlur={this.props.onNameBlur}
            />
            <span className="display-inline-block align-m type-s type-weak mrm">
              from the trigger text, or by asking a question:
            </span>
          </div>
          <div className="column column-shrink">
            <DeleteButton
              onClick={this.onDeleteClick}
              title={this.props.name ? `Delete the “${this.props.name}” input` : "Delete this input"}
            />
          </div>
        </div>
        <div className="columns columns-elastic">
          <div className="column column-expand">
            <Input
              id={"question" + this.props.id}
              ref="question"
              placeholder="Write a question to ask the user for this input"
              autoFocus={this.props.shouldGrabFocus}
              value={this.props.question}
              onChange={this.onQuestionChange}
              onEnterKey={this.props.onEnterKey}
              className="form-input-borderless type-italic"
            />
          </div>
          <div className="column column-shrink plsymbol"></div>
        </div>
      </div>
    );
  }
});

});
