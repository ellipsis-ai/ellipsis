define(function(require) {
  var React = require('react'),
    Input = require('../models/input');

  return React.createClass({
    displayName: 'SavedAnswerEditor',
    propTypes: {
      onToggle: React.PropTypes.func.isRequired,
      savedAnswers: React.PropTypes.arrayOf(
        React.PropTypes.shape({
          inputId: React.PropTypes.string.isRequired,
          userAnswerCount: React.PropTypes.number.isRequired,
          myValueString: React.PropTypes.string
        })
      ).isRequired,
      selectedInput: React.PropTypes.instanceOf(Input),
      onForgetSavedAnswerForUser: React.PropTypes.func.isRequired,
      onForgetSavedAnswersForTeam: React.PropTypes.func.isRequired
    },

    getSavedAnswerFor: function(input) {
      return this.props.savedAnswers.find((ea) => ea.inputId === input.inputId);
    },

    otherUsersAnswersSaved: function(answer) {
      return answer.userAnswerCount > 1 ||
        (!answer.myValueString && answer.userAnswerCount > 0);
    },

    isSavedForTeam: function() {
      return this.props.selectedInput && this.props.selectedInput.isSavedForTeam;
    },

    isSavedForUser: function() {
      return this.props.selectedInput && this.props.selectedInput.isSavedForUser;
    },

    forgetUserAnswer: function(answer) {
      this.props.onForgetSavedAnswerForUser(answer.inputId);
    },

    forgetTeamAnswers: function(answer) {
      this.props.onForgetSavedAnswersForTeam(answer.inputId);
    },

    renderForgetUserAnswerButton: function(answer) {
      return (
        <button type="button" className="mrs mbs"
          onClick={this.forgetUserAnswer.bind(this, answer)}
          disabled={!answer.myValueString}
        >
          Forget your answer
        </button>
      );
    },

    renderForgetTeamAnswersButton: function(answer) {
      return (
        <button type="button" className="mrs mbs"
          disabled={answer.userAnswerCount === 0}
          onClick={this.forgetTeamAnswers.bind(this, answer)}
        >
          {this.otherUsersAnswersSaved(answer) ? "Forget all answers" : "Forget this answer"}
        </button>
      );
    },

    renderForgetButtons: function(answer) {
      var includeTeamButton = this.isSavedForTeam() ||
        this.isSavedForUser() && this.otherUsersAnswersSaved(answer);
      if (answer) {
        return (
          <span>
            {this.isSavedForUser() ? this.renderForgetUserAnswerButton(answer) : null}
            {includeTeamButton ? this.renderForgetTeamAnswersButton(answer) : null}
          </span>
        );
      }
    },

    render: function() {
      var selectedInput = this.props.selectedInput;
      var answer = selectedInput && this.getSavedAnswerFor(selectedInput);
      return (
        <div>
          <div className="box-action phn">
            <div className="container">
              <div className="columns">
                <div className="column column-page-sidebar">
                  <h4 className="type-weak">{selectedInput ? (
                      <span>
                        <span>Saved answers for </span>
                        <span className="type-monospace type-regular">{selectedInput.name}</span>
                      </span>
                    ) : (
                      <span>Saved answers</span>
                    )}</h4>
                </div>
                <div className="column column-page-main">

                  {selectedInput && answer ? (
                    <div className="type-s">

                      <div className="mbl">
                        <h6>Question:</h6>
                        <div><i>{selectedInput.question}</i></div>
                      </div>

                      {this.isSavedForUser() ? (
                        <div className="mbl">
                          <h6 className="display-inline-block mbn">Total number of answers:</h6>
                          <span>
                            <span className="type-bold"> {answer.userAnswerCount}</span>
                            {answer.userAnswerCount === 1 && answer.myValueString ? (
                              <span className="type-weak"> (yours)</span>
                            ) : null}
                          </span>
                        </div>
                      ) : null}

                      <div className="mbl">
                        <h6 className="display-inline-block mbn mrs align-m">{this.isSavedForTeam() ? "Saved answer for the team:" : "Your saved answer:" }</h6>
                        <div className="box-code-example display-inline-block align-m" style={{
                          maxHeight: "6.5em",
                          overflow: "auto"
                        }}>
                          {answer.myValueString || (<span className="type-disabled">(none)</span>)}
                        </div>
                      </div>

                    </div>

                  ) : null}

                  <div className="mtxl">
                    <button type="button" className="button-primary mrs mbs" onClick={this.props.onToggle}>Done</button>
                    {this.renderForgetButtons(answer)}
                  </div>

                </div>
              </div>
            </div>
          </div>
        </div>
      );
    }
  });
});
