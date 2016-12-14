define(function(require) {
  var React = require('react'),
    Param = require('../models/param');

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
      selectedParam: React.PropTypes.instanceOf(Param),
      onForgetSavedAnswerForUser: React.PropTypes.func.isRequired,
      onForgetSavedAnswersForTeam: React.PropTypes.func.isRequired
    },

    getSavedAnswerFor: function(param) {
      return this.props.savedAnswers.find((ea) => ea.inputId === param.inputId);
    },

    otherUsersAnswersSaved: function(answer) {
      return answer.userAnswerCount > 1 ||
        (!answer.myValueString && answer.userAnswerCount > 0);
    },

    forgetUserAnswer: function(answer) {
      this.props.onForgetSavedAnswerForUser(answer.inputId);
    },

    forgetTeamAnswers: function(answer) {
      this.props.onForgetSavedAnswersForTeam(answer.inputId);
    },

    renderForgetUserAnswerButton: function(answer) {
      return (
        <button type="button" className="button-s mbs"
          onClick={this.forgetUserAnswer.bind(this, answer)}
          disabled={!answer.myValueString}
        >
          Forget this answer
        </button>
      );
    },

    renderForgetTeamAnswersButton: function(answer) {
      return (
        <button type="button" className="button-s mbs"
          disabled={answer.userAnswerCount === 0}
          onClick={this.forgetTeamAnswers.bind(this, answer)}
        >
          {this.otherUsersAnswersSaved(answer) ? "Forget everyone’s answers" : "Forget this answer"}
        </button>
      );
    },

    render: function() {
      var selectedParam = this.props.selectedParam;
      var answer = selectedParam && this.getSavedAnswerFor(selectedParam);
      return (
        <div>
          <div className="box-action phn">
            <div className="container">
              <div className="columns">
                <div className="column column-page-sidebar">
                  <h4 className="type-weak">{selectedParam ? (
                      <span>
                        <span>Saved answers for </span>
                        <span className="type-monospace type-regular">{selectedParam.name}</span>
                      </span>
                    ) : (
                      <span>Saved answers</span>
                    )}</h4>
                </div>
                <div className="column column-page-main">

                  {selectedParam && answer ? (
                    <div className="type-s">

                      <div className="mbl">
                        <h6>Question:</h6>
                        <div><i>{selectedParam.question}</i></div>
                      </div>

                      {selectedParam.isSavedForUser ? (
                        <div className="mbl">
                          <h6 className="display-inline-block mbn">Total number of answers:</h6>
                          <span>
                            <span className="type-bold"> {answer.userAnswerCount}</span>
                            {answer.userAnswerCount === 1 && answer.myValueString ? (
                              <span className="type-weak"> (yours)</span>
                            ) : null}
                          </span>
                          <div className="mtxs">
                            {this.otherUsersAnswersSaved(answer) ? this.renderForgetTeamAnswersButton(answer) : null}
                          </div>
                        </div>
                      ) : null}

                      <div className="mbl">
                        <h6>{selectedParam.isSavedForTeam ? "Saved answer for the team:" : "Your saved answer:" }</h6>
                        <div className="box-code-example display-inline-block mbs mrm">
                          {answer.myValueString || (<span className="type-disabled">(none)</span>)}
                        </div>

                        {selectedParam.isSavedForUser ? this.renderForgetUserAnswerButton(answer) : this.renderForgetTeamAnswersButton(answer)}
                      </div>

                    </div>

                  ) : null}

                  <div className="mtxl">
                    <button type="button" onClick={this.props.onToggle}>
                      Done
                    </button>
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
