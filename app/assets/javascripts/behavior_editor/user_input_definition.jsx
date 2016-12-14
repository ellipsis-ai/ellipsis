define(function(require) {
var React = require('react'),
  DeleteButton = require('./delete_button'),
  Input = require('../form/input'),
  Select = require('../form/select'),
  SVGTip = require('../svg/tip'),
  Param = require('../models/param'),
  ifPresent = require('../if_present');

  var EACH_TIME = "each_time";
  var PER_TEAM = "per_team";
  var PER_USER = "per_user";

return React.createClass({
  displayName: 'UserInputDefinition',
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
    shouldGrabFocus: React.PropTypes.bool,
    savedAnswers: React.PropTypes.shape({
      myValueString: React.PropTypes.string,
      userAnswerCount: React.PropTypes.number.isRequired
    }),
    onToggleSavedAnswer: React.PropTypes.func.isRequired
  },

  onNameChange: function(newName) {
    this.props.onChange(this.props.param.clone({ name: Param.formatName(newName) }));
  },

  onParamTypeChange: function(newTypeId) {
    var newType = this.props.paramTypes.find(ea => ea.id === newTypeId);
    this.props.onChange(this.props.param.clone({ paramType: newType }));
  },

  onQuestionChange: function(newQuestion) {
    this.props.onChange(this.props.param.clone({ question: newQuestion }));
  },

  onSaveOptionChange: function(newOption) {
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

  configureType: function() {
    window.location.href = jsRoutes.controllers.BehaviorEditorController.edit(this.props.param.paramType.id).url;
  },

  isConfigurable: function() {
    const pt = this.props.param.paramType;
    return pt.id !== pt.name;
  },

  focus: function() {
    this.refs.name.focus();
    this.refs.name.select();
  },

  keyFor: function(paramType) {
    return 'param-type-' + this.props.id + '-' + paramType.id;
  },

  getParamSource: function() {
    var message;
    if (this.props.numLinkedTriggers === 1) {
      message = "from 1 trigger above, or by asking a question:";
    } else if (this.props.numLinkedTriggers > 1) {
      message = `from ${this.props.numLinkedTriggers} triggers above, or by asking a question:`;
    } else {
      message = "by asking a question:";
    }
    return (
      <span className="display-inline-block align-m type-s type-weak mrm fade-in">{message}</span>
    );
  },

  getSaveOptionValue: function() {
    if (this.props.param.isSavedForTeam) {
      return PER_TEAM;
    } else if (this.props.param.isSavedForUser) {
      return PER_USER;
    } else {
      return EACH_TIME;
    }
  },

  getSavedAnswerCount: function() {
    return this.props.savedAnswers ?
      this.props.savedAnswers.userAnswerCount : 0;
  },

  onToggleSavedAnswer: function() {
    this.props.onToggleSavedAnswer(this.props.param.inputId);
  },

  renderSavedInfo: function() {
    var isShared = this.props.param.isShared();
    var savedAnswerCount = this.getSavedAnswerCount();
    if (isShared || savedAnswerCount > 0) {
      return (
        <div className="box-tip mbneg1">
          {savedAnswerCount > 0 ? (
            <span className="type-s mrm">
              <button type="button" className="button-s button-shrink" onClick={this.onToggleSavedAnswer}>
                {savedAnswerCount === 1 ? "1 saved answer" : `${savedAnswerCount} saved answers`}
              </button>
            </span>
          ) : null}
          {isShared ? (
            <span>
              <span className="display-inline-block align-b type-pink mrs" style={{ height: 24 }}>
                <SVGTip />
              </span>
              <span className="type-s mrm">This input is shared with other actions.</span>
            </span>
          ) : null}
        </div>
      );
    }
  },

  render: function() {
    return (
      <div className="border border-light">
        <div className="bg-white plm pbxs">
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
              {this.getParamSource()}
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
              className="form-input-borderless"
            />
          </div>
          <div className="prsymbol mts">
            <Select className="form-select-s form-select-light align-m mrm mbs" name="paramType" value={this.getSaveOptionValue()} onChange={this.onSaveOptionChange}>
              <option value={EACH_TIME}>
                Ask each time the skill is run
              </option>
              <option value={PER_TEAM}>
                Ask once, save the answer for the whole team
              </option>
              <option value={PER_USER}>
                Ask each user once, save their answers
              </option>
            </Select>
            <span className="display-inline-block align-m type-s type-weak mrm mbs">and allow data type</span>
            <Select className="form-select-s form-select-light align-m mrm mbs" name="paramType" value={this.props.param.paramType.id} onChange={this.onParamTypeChange}>
              {this.props.paramTypes.map((paramType) => (
                <option value={paramType.id} key={this.keyFor(paramType)}>
                  {paramType.name}
                </option>
              ))}
            </Select>
            {ifPresent(this.isConfigurable(), () => (
              <button type="button" className="button-s button-shrink mbs" onClick={this.configureType}>Edit type…</button>
            ))}
          </div>
        </div>
        {this.renderSavedInfo()}
      </div>
    );
  }
});

});
