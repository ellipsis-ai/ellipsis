import * as React from 'react';
import Input from '../models/input';
import autobind from "../lib/autobind";
import {SavedAnswer} from "./user_input_configuration";

interface Props {
  onToggle: () => void,
  savedAnswers: Array<SavedAnswer>
  selectedInput: Input
  onForgetSavedAnswerForUser: (inputId: string) => void,
  onForgetSavedAnswersForTeam: (inputId: string) => void
}

class SavedAnswerEditor extends React.Component<Props> {
    constructor(props) {
      super(props);
      autobind(this);
    }

    getSavedAnswerFor(input: Input): Option<SavedAnswer> {
      return this.props.savedAnswers.find((ea) => ea.inputId === input.inputId);
    }

    otherUsersAnswersSaved(answer: SavedAnswer): boolean {
      return answer.userAnswerCount > 1 || (!answer.myValueString && answer.userAnswerCount > 0);
    }

    isSavedForTeam(): boolean {
      return this.props.selectedInput && this.props.selectedInput.isSavedForTeam;
    }

    isSavedForUser(): boolean {
      return this.props.selectedInput && this.props.selectedInput.isSavedForUser;
    }

    forgetUserAnswer(answer: SavedAnswer): void {
      this.props.onForgetSavedAnswerForUser(answer.inputId);
    }

    forgetTeamAnswers(answer: SavedAnswer): void {
      this.props.onForgetSavedAnswersForTeam(answer.inputId);
    }

    renderForgetUserAnswerButton(answer: SavedAnswer) {
      return (
        <button type="button" className="mrs mbs"
          onClick={this.forgetUserAnswer.bind(this, answer)}
          disabled={!answer.myValueString}
        >
          Forget your answer
        </button>
      );
    }

    renderForgetTeamAnswersButton(answer: SavedAnswer) {
      return (
        <button type="button" className="mrs mbs"
          disabled={answer.userAnswerCount === 0}
          onClick={this.forgetTeamAnswers.bind(this, answer)}
        >
          {this.otherUsersAnswersSaved(answer) ? "Forget all answers" : "Forget this answer"}
        </button>
      );
    }

    renderForgetButtons(answer: Option<SavedAnswer>) {
      const includeTeamButton = this.isSavedForTeam() || this.isSavedForUser() && answer && this.otherUsersAnswersSaved(answer);
      if (answer) {
        return (
          <span>
            {this.isSavedForUser() ? this.renderForgetUserAnswerButton(answer) : null}
            {includeTeamButton ? this.renderForgetTeamAnswersButton(answer) : null}
          </span>
        );
      } else {
        return null;
      }
    }

    render() {
      const selectedInput = this.props.selectedInput;
      const answer = selectedInput && this.getSavedAnswerFor(selectedInput);
      return (
        <div>
          <div className="box-action phn">
            <div className="container">
              <div className="columns">
                <div className="column column-page-sidebar">
                  <h4 className="mtn type-weak">{selectedInput ? (
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
}

export default SavedAnswerEditor;
