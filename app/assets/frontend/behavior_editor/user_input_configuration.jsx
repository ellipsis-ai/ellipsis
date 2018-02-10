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

const UserInputConfiguration = React.createClass({
    propTypes: {
      onInputChange: React.PropTypes.func.isRequired,
      onInputMove: React.PropTypes.func.isRequired,
      onInputDelete: React.PropTypes.func.isRequired,
      onInputAdd: React.PropTypes.func.isRequired,
      onInputNameFocus: React.PropTypes.func.isRequired,
      onInputNameBlur: React.PropTypes.func.isRequired,
      userInputs: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Input)).isRequired,
      paramTypes: React.PropTypes.arrayOf(React.PropTypes.instanceOf(ParamType)).isRequired,
      triggers: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Trigger)).isRequired,
      hasSharedAnswers: React.PropTypes.bool.isRequired,
      otherBehaviorsInGroup: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      onToggleSharedAnswer: React.PropTypes.func.isRequired,
      savedAnswers: React.PropTypes.arrayOf(
        React.PropTypes.shape({
          inputId: React.PropTypes.string.isRequired,
          userAnswerCount: React.PropTypes.number.isRequired,
          myValueString: React.PropTypes.string
        })
      ).isRequired,
      onToggleSavedAnswer: React.PropTypes.func.isRequired,
      onConfigureType: React.PropTypes.func.isRequired,
      onToggleInputHelp: React.PropTypes.func.isRequired,
      helpInputVisible: React.PropTypes.bool.isRequired
    },

    componentDidUpdate: function(prevProps) {
      if (this.props.userInputs.length > prevProps.userInputs.length) {
        this.focusIndex(this.props.userInputs.length - 1);
      }
    },

    swapButtons: [],

    onChange: function(index, data) {
      this.props.onInputChange(index, data);
    },
    onDelete: function(index) {
      this.props.onInputDelete(index);
    },
    onEnterKey: function(index) {
      if (index + 1 < this.props.userInputs.length) {
        this.focusIndex(index + 1);
      } else if (this.props.userInputs[index].question) {
        this.addInput();
      }
    },

    onNameFocus: function(index) {
      this.props.onInputNameFocus(index);
    },

    onNameBlur: function(index) {
      this.props.onInputNameBlur(index);
    },

    addInput: function() {
      this.props.onInputAdd();
    },

    focusIndex: function(index) {
      this.refs['input' + index].focus();
    },

    isShared: function(input) {
      const firstBehaviorWithSameInput = this.props.otherBehaviorsInGroup.find(behavior => {
        return behavior.inputIds.indexOf(input.inputId) !== -1;
      });
      return !!firstBehaviorWithSameInput;
    },

    countLinkedTriggersForInput: function(inputName, inputIndex) {
      return this.props.triggers.filter((trigger) => trigger.usesInputName(inputName) || trigger.capturesInputIndex(inputIndex)).length;
    },

    getSavedAnswersFor: function(inputId) {
      return this.props.savedAnswers.find((answers) => answers.inputId === inputId);
    },

    renderReuseInput: function(optionalProperties) {
      var buttonProps = Object.assign({}, optionalProperties);
      if (this.props.hasSharedAnswers) {
        return (
          <Button
            className={"button-s " + (buttonProps.className || "")}
            onClick={this.props.onToggleSharedAnswer}
          >
            Use a saved answer from another action…
          </Button>
        );
      } else {
        return null;
      }
    },

    moveInputDown: function(index) {
      this.props.onInputMove(index, index + 1);
      if (this.swapButtons[index]) {
        this.swapButtons[index].blur();
      }
    },

    render: function() {
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
                          ref={'input' + inputIndex}
                          input={input}
                          isShared={this.isShared(input)}
                          paramTypes={this.props.paramTypes}
                          onChange={this.onChange.bind(this, inputIndex)}
                          onDelete={this.onDelete.bind(this, inputIndex)}
                          onEnterKey={this.onEnterKey.bind(this, inputIndex)}
                          onNameFocus={this.onNameFocus.bind(this, inputIndex)}
                          onNameBlur={this.onNameBlur.bind(this, inputIndex)}
                          numLinkedTriggers={this.countLinkedTriggersForInput(input.name, inputIndex)}
                          id={inputIndex}
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
                    {this.renderReuseInput({ className: "mbs" })}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    }
});

export default UserInputConfiguration;

