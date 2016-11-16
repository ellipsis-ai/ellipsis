define(function(require) {
var React = require('react'),
  DeleteButton = require('./delete_button'),
  Input = require('../form/input'),
  Param = require('../models/param');

  var paramTypeDescriptions = {
    "Text": "Some text",
    "Number": "A number"
  };

  var EACH_TIME = "each_time";
  var PER_TEAM = "per_team";
  var PER_USER = "per_user";

return React.createClass({
  propTypes: {
    id: React.PropTypes.oneOfType([
      React.PropTypes.number,
      React.PropTypes.string
    ]).isRequired,
    param: React.PropTypes.instanceOf(Param).isRequired,
    paramTypes: React.PropTypes.arrayOf(
      React.PropTypes.shape({
        id: React.PropTypes.string,
        name: React.PropTypes.string
      })
    ).isRequired,
    onChange: React.PropTypes.func.isRequired,
    onDelete: React.PropTypes.func.isRequired,
    onEnterKey: React.PropTypes.func.isRequired,
    onNameFocus: React.PropTypes.func.isRequired,
    onNameBlur: React.PropTypes.func.isRequired,
    numLinkedTriggers: React.PropTypes.number.isRequired,
    shouldGrabFocus: React.PropTypes.bool
  },

  onNameChange: function(newName) {
    this.props.onChange(this.props.param.clone({ name: Param.formatName(newName) }));
  },

  onParamTypeChange: function(event) {
    var newTypeId = event.target.value;
    var newType = this.props.paramTypes.find(ea => ea.id === newTypeId);
    this.props.onChange(this.props.param.clone({ paramType: newType }));
  },

  onQuestionChange: function(newQuestion) {
    this.props.onChange(this.props.param.clone({ question: newQuestion }));
  },

  onSaveOptionChange: function(event) {
    var newOption = event.target.value;
    var changedProps = { isSavedForTeam: false, isSavedForUser: false };
    if (newOption === PER_TEAM) {
      changedProps.isSavedForTeam = true;
    } else if (newOption === PER_USER) {
      changedProps.isSavedForUser = true;
    }
    this.props.onChange(this.props.param.clone(changedProps));
  },

  onDeleteClick: function() {
    this.props.onDelete();
  },

  focus: function() {
    this.refs.name.focus();
    this.refs.name.select();
  },

  keyFor: function(paramType) {
    return 'param-type-' + this.props.id + '-' + paramType.id;
  },

  paramTypeDisplayNameFor: function(paramTypeName) {
    return paramTypeDescriptions[paramTypeName] || paramTypeName;
  },

  getParamSource: function() {
    if (this.props.numLinkedTriggers === 1) {
      return "from 1 trigger above, or by";
    } else if (this.props.numLinkedTriggers > 1) {
      return `from ${this.props.numLinkedTriggers} triggers above, or by`;
    } else {
      return "by";
    }
  },

  getSaveOptionValue: function() {
    if (this.props.param.isSavedForTeam) {
      return PER_TEAM;
    } else if (this.props.param.isSavedForUser) {
      return PER_USER;
    } else {
      return EACH_TIME
    }
  },

  render: function() {
    return (
      <div>
        <div className="columns columns-elastic">
          <div className="column column-expand align-form-input">
            <span className="display-inline-block align-m type-s type-weak mrm">Collect</span>
            <Input
              ref="name"
              className="form-input-borderless type-monospace type-s width-10 mrm"
              placeholder="userInput"
              value={this.props.param.name}
              onChange={this.onNameChange}
              onFocus={this.props.onNameFocus}
              onBlur={this.props.onNameBlur}
            />
            <span className="display-inline-block align-m type-s type-weak mrm">
              {this.getParamSource()}
            </span>
            <select className="form-select form-select-s min-width-10 align-m mrm" name="paramType" value={this.getSaveOptionValue()} onChange={this.onSaveOptionChange}>
              <option value={EACH_TIME} key={EACH_TIME}>
                asking each time the skill is run
              </option>
              <option value={PER_TEAM} key={PER_TEAM}>
                asking once and saving the answer for the whole team
              </option>
              <option value={PER_USER} key={PER_USER}>
                asking each user once and saving their answer
              </option>
            </select>
          </div>
          <div className="column column-shrink">
            <DeleteButton
              onClick={this.onDeleteClick}
              title={this.props.param.name ? `Delete the “${this.props.param.name}” input` : "Delete this input"}
            />
          </div>
        </div>
        <div className="prsymbol">
          <Input
            id={"question" + this.props.id}
            ref="question"
            placeholder="Write a question to ask the user for this input"
            autoFocus={this.props.shouldGrabFocus}
            value={this.props.param.question}
            onChange={this.onQuestionChange}
            onEnterKey={this.props.onEnterKey}
            className="form-input-borderless type-italic"
          />
        </div>
        <div className="column column-expand align-form-input">
          <span className="display-inline-block align-m type-s type-weak mrm">and accept</span>
          <select className="form-select form-select-s min-width-10 align-m mrm" name="paramType" value={this.props.param.paramType.id} onChange={this.onParamTypeChange}>
            {this.props.paramTypes.map((paramType) => (
              <option value={paramType.id} key={this.keyFor(paramType)}>
                {this.paramTypeDisplayNameFor(paramType.name)}
              </option>
            ))}
          </select>
          <span className="display-inline-block align-m type-s type-weak mrm">in response</span>
        </div>
      </div>
    );
  }
});

});
