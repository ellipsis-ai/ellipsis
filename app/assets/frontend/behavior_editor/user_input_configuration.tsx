import * as React from 'react';
import Button from '../form/button';
import HelpButton from '../help/help_button';
import SVGSwap from '../svg/swap';
import SectionHeading from '../shared_ui/section_heading';
import UserInputDefinition from './user_input_definition';
import BehaviorVersion from '../models/behavior_version';
import Input from '../models/input';
import ParamType from '../models/param_type';
import Trigger from '../models/trigger';
import autobind from "../lib/autobind";

export interface SavedAnswer {
  inputId: string,
  userAnswerCount: number,
  myValueString?: Option<string>
}

interface Props {
  onInputChange: (inputIndex: number, newInput: Input) => void,
  onInputMove: (oldIndex: number, newIndex: number) => void,
  onInputDelete: (inputIndex: number) => void,
  onInputAdd: (optionalNewName?: Option<string>, optionalCallback?: () => void) => void,
  onInputNameFocus: (inputIndex: number) => void,
  onInputNameBlur: (inputIndex: number) => void,
  userInputs: Array<Input>,
  paramTypes: Array<ParamType>,
  triggers: Array<Trigger>,
  hasSharedAnswers: boolean,
  otherBehaviorsInGroup: Array<BehaviorVersion>,
  onToggleSharedAnswer: () => void,
  savedAnswers: Array<SavedAnswer>,
  onToggleSavedAnswer: (savedAnswerId: string) => void,
  onConfigureType: (paramTypeId: string) => void,
  onToggleInputHelp: () => void,
  helpInputVisible: boolean
}

class UserInputConfiguration extends React.Component<Props> {
    swapButtons: Array<Option<Button>>;
    inputs: Array<Option<UserInputDefinition>>;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.swapButtons = [];
      this.inputs = [];
    }

    componentDidUpdate(prevProps: Props): void {
      if (this.props.userInputs.length > prevProps.userInputs.length) {
        this.focusIndex(this.props.userInputs.length - 1);
      }
    }

    onChange(index: number, data: Input): void {
      this.props.onInputChange(index, data);
    }

    onDelete(index: number): void {
      this.props.onInputDelete(index);
    }

    onEnterKey(index: number): void {
      if (index + 1 < this.props.userInputs.length) {
        this.focusIndex(index + 1);
      } else if (this.props.userInputs[index].question) {
        this.addInput();
      }
    }

    onNameFocus(index: number): void {
      this.props.onInputNameFocus(index);
    }

    onNameBlur(index: number): void {
      this.props.onInputNameBlur(index);
    }

    addInput(): void {
      this.props.onInputAdd();
    }

    focusIndex(index: number): void {
      const input = this.inputs[index];
      if (input) {
        input.focus();
      }
    }

    isShared(input: Input): boolean {
      const firstBehaviorWithSameInput = this.props.otherBehaviorsInGroup.find((behavior) => {
        return Boolean(input.inputId && behavior.inputIds.indexOf(input.inputId) !== -1);
      });
      return Boolean(firstBehaviorWithSameInput);
    }

    countLinkedTriggersForInput(inputName: string, inputIndex: number): number {
      return this.props.triggers.filter((trigger) => trigger.usesInputName(inputName) || trigger.capturesInputIndex(inputIndex)).length;
    }

    getSavedAnswersFor(inputId: Option<string>): Option<SavedAnswer> {
      return inputId ? this.props.savedAnswers.find((answers) => answers.inputId === inputId) : null;
    }

    renderReuseInput() {
      if (this.props.hasSharedAnswers) {
        return (
          <Button
            className="button-s mbs"
            onClick={this.props.onToggleSharedAnswer}
          >
            Use a saved answer from another action…
          </Button>
        );
      } else {
        return null;
      }
    }

    moveInputDown(index: number): void {
      this.props.onInputMove(index, index + 1);
      const button = this.swapButtons[index];
      if (button) {
        button.blur();
      }
    }

    render() {
      return (
        <div>
          <div>
            <hr className="mtn rule-subtle" />

            <div className="columns container container-narrow">
              <div className="mbxxl">
                <div>
                  <SectionHeading number="2">
                    <span className="mrm">Inputs</span>
                    <span className="display-inline-block">
                      <HelpButton onClick={this.props.onToggleInputHelp} toggled={this.props.helpInputVisible} />
                    </span>
                  </SectionHeading>
                  <div className="mbm">
                    {this.props.userInputs.map((input, inputIndex) => (
                      <div key={`userInput${inputIndex}`}>
                        <UserInputDefinition
                          key={'UserInputDefinition' + inputIndex}
                          ref={(el) => this.inputs[inputIndex] = el}
                          input={input}
                          isShared={this.isShared(input)}
                          paramTypes={this.props.paramTypes}
                          onChange={this.onChange.bind(this, inputIndex)}
                          onDelete={this.onDelete.bind(this, inputIndex)}
                          onEnterKey={this.onEnterKey.bind(this, inputIndex)}
                          onNameFocus={this.onNameFocus.bind(this, inputIndex)}
                          onNameBlur={this.onNameBlur.bind(this, inputIndex)}
                          numLinkedTriggers={this.countLinkedTriggersForInput(input.name, inputIndex)}
                          id={`userInput${inputIndex}`}
                          savedAnswers={this.getSavedAnswersFor(input.inputId)}
                          onToggleSavedAnswer={this.props.onToggleSavedAnswer}
                          onConfigureType={this.props.onConfigureType}
                        />
                        {inputIndex + 1 < this.props.userInputs.length ? (
                          <div className="align-c pvxs type-weak">
                            <Button
                              ref={(el) => this.swapButtons[inputIndex] = el}
                              className="button-s button-subtle button-symbol type-label"
                              onClick={this.moveInputDown.bind(this, inputIndex)}
                              title="Swap the order of the input above with the one below"
                            >
                              <SVGSwap />
                            </Button>
                          </div>
                        ) : null}
                      </div>
                    ))}
                  </div>
                  <div>
                    <Button className="button-s mrm mbs" onClick={this.addInput}>
                      Add an input
                    </Button>
                    {this.renderReuseInput()}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    }
}

export default UserInputConfiguration;

