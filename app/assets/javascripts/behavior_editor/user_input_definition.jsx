define(function(require) {
var React = require('react'),
  DeleteButton = require('../shared_ui/delete_button'),
  FormInput = require('../form/input'),
  Select = require('../form/select'),
  SVGTip = require('../svg/tip'),
  SVGInfo = require('../svg/info'),
  Input = require('../models/input'),
  ifPresent = require('../lib/if_present');

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
    input: React.PropTypes.instanceOf(Input).isRequired,
    isShared: React.PropTypes.bool.isRequired,
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
    onToggleSavedAnswer: React.PropTypes.func.isRequired,
    onConfigureType: React.PropTypes.func.isRequired
  },

  onNameChange: function(newName) {
    this.props.onChange(this.props.input.clone({ name: Input.formatName(newName) }));
  },

  onInputTypeChange: function(newTypeId) {
    var newType = this.props.paramTypes.find(ea => ea.id === newTypeId);
    this.props.onChange(this.props.input.clone({ paramType: newType }));
  },

  onQuestionChange: function(newQuestion) {
    this.props.onChange(this.props.input.clone({ question: newQuestion }));
  },

  onSaveOptionChange: function(newOption) {
    var changedProps = { isSavedForTeam: false, isSavedForUser: false };
    if (newOption === PER_TEAM) {
      changedProps.isSavedForTeam = true;
    } else if (newOption === PER_USER) {
      changedProps.isSavedForUser = true;
    }
    this.props.onChange(this.props.input.clone(changedProps));
  },

  onDeleteClick: function() {
    this.props.onDelete();
  },

  onConfigureType: function() {
    this.props.onConfigureType(this.props.input.paramType.id);
  },

  isConfigurable: function() {
    const pt = this.props.input.paramType;
    return pt.id !== pt.name;
  },

  focus: function() {
    this.refs.name.focus();
    this.refs.name.select();
  },

  keyFor: function(paramType) {
    return 'param-type-' + this.props.id + '-' + paramType.id;
  },

  getInputSource: function() {
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
    if (this.props.input.isSavedForTeam) {
      return PER_TEAM;
    } else if (this.props.input.isSavedForUser) {
      return PER_USER;
    } else {
      return EACH_TIME;
    }
  },

  getSavedAnswerCount: function() {
    return this.props.savedAnswers ?
      this.props.savedAnswers.userAnswerCount : 0;
  },

  inputSavesAnswers: function() {
    return this.props.input.isSavedForTeam ||
        this.props.input.isSavedForUser;
  },

  getSavedAnswerSummary: function() {
    var answer = this.props.savedAnswers;
    var count = this.getSavedAnswerCount();
    var userHasAnswered = answer && answer.myValueString;
    if (this.props.input.isSavedForTeam) {
      if (count > 0) {
        return "1 answer saved";
      } else {
        return "No answer saved yet";
      }
    } else {
      if (count === 1 && userHasAnswered) {
        return "1 answer saved (yours)";
      } else if (count === 1 && !userHasAnswered) {
        return "1 answer saved (another person)";
      } else if (count === 2 && userHasAnswered) {
        return "2 answers saved (including yours)";
      } else if (count > 2 && userHasAnswered) {
        return `${count} answers saved (including yours)`;
      } else if (count > 1) {
        return `${count} answers saved (for other people)`;
      } else {
        return "No answers saved yet";
      }
    }
  },

  onToggleSavedAnswer: function() {
    this.props.onToggleSavedAnswer(this.props.input.inputId);
  },

  renderSavedAnswerInfo: function() {
    if (this.inputSavesAnswers()) {
      return (
        <div className="type-s mtxs mrm mbs">
          <span className="display-inline-block align-b type-pink mrs" style={{ height: 24 }}>
            <SVGInfo />
          </span>
          <span className="mrs type-weak">{this.getSavedAnswerSummary()}</span>
          <button type="button" className="button-s button-shrink"
            disabled={this.getSavedAnswerCount() === 0}
            onClick={this.onToggleSavedAnswer}>
            Details
          </button>
        </div>
      );
    }
  },

  renderSharingInfo: function() {
    if (this.props.isShared) {
      return (
        <div className="box-tip mtneg1 mbneg1 phs border-left border-right">
          <span className="display-inline-block align-b type-green mrs" style={{ height: 24 }}>
            <SVGTip />
          </span>
          <span className="type-s mrm">This input is shared with other actions.</span>
        </div>
      );
    }
  },

  render: function() {
    return (
      <div>
        <div className="border border-light bg-white plm pbxs">
          <div className="columns columns-elastic">
            <div className="column column-expand align-form-input">
              <span className="display-inline-block align-m type-s type-weak mrm">Collect</span>
              <FormInput
                ref="name"
                className="form-input-borderless type-monospace type-s width-15 mrm"
                placeholder="userInput"
                value={this.props.input.name}
                onChange={this.onNameChange}
                onFocus={this.props.onNameFocus}
                onBlur={this.props.onNameBlur}
              />
              {this.getInputSource()}
            </div>
            <div className="column column-shrink">
              <DeleteButton
                onClick={this.onDeleteClick}
                title={this.props.input.name ? `Delete the “${this.props.input.name}” input` : "Delete this input"}
              />
            </div>
          </div>
          <div className="prsymbol">
            <FormInput
              id={"question" + this.props.id}
              ref="question"
              placeholder="Write a question to ask the user for this input"
              autoFocus={this.props.shouldGrabFocus}
              value={this.props.input.question}
              onChange={this.onQuestionChange}
              onEnterKey={this.props.onEnterKey}
              className="form-input-borderless"
            />
          </div>
          <div className="prsymbol mts">
            <Select className="form-select-s form-select-light align-m mrm mbs" name="paramType" value={this.getSaveOptionValue()} onChange={this.onSaveOptionChange}>
              <option value={EACH_TIME}>
                Ask each time action is triggered
              </option>
              <option value={PER_TEAM}>
                Ask once for the team, then re-use the answer
              </option>
              <option value={PER_USER}>
                Ask once for each user, then re-use the answer
              </option>
            </Select>
            <span className="display-inline-block align-m type-s type-weak mrm mbs">and allow data type</span>
            <Select className="form-select-s form-select-light align-m mrm mbs" name="paramType" value={this.props.input.paramType.id} onChange={this.onInputTypeChange}>
              {this.props.paramTypes.map((paramType) => (
                <option value={paramType.id} key={this.keyFor(paramType)}>
                  {paramType.name}
                </option>
              ))}
            </Select>
            {ifPresent(this.isConfigurable(), () => (
              <button type="button" className="button-s button-shrink mbs" onClick={this.onConfigureType}>Edit type…</button>
            ))}
          </div>
          {this.renderSavedAnswerInfo()}
        </div>
        {this.renderSharingInfo()}
      </div>
    );
  }
});

});
