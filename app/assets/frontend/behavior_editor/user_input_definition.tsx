import * as React from 'react';
import DeleteButton from '../shared_ui/delete_button';
import FormInput from '../form/input';
import Formatter from '../lib/formatter';
import Select, {SelectOption} from '../form/select';
import SVGTip from '../svg/tip';
import SVGInfo from '../svg/info';
import Input from '../models/input';
import ParamType from '../models/param_type';
import autobind from "../lib/autobind";
import {SavedAnswer} from "./user_input_configuration";

enum InputSaveOption {
  EACH_TIME = "each_time",
  PER_TEAM = "per_team",
  PER_USER = "per_user"
}

interface Props {
  id: string
  input: Input,
  isShared: boolean,
  paramTypes: Array<ParamType>,
  onChange: (newInput: Input) => void,
  onDelete: () => void,
  onEnterKey: () => void,
  onNameFocus: () => void,
  onNameBlur: () => void,
  numLinkedTriggers: number,
  savedAnswers: Option<SavedAnswer>,
  onToggleSavedAnswer: (inputId: string) => void,
  onConfigureType: (paramTypeId: string) => void,
}

class UserInputDefinition extends React.Component<Props> {
  nameInput: Option<FormInput>;

  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  onNameChange(newName: string): void {
    this.props.onChange(this.props.input.clone({ name: Formatter.formatNameForCode(newName) }));
  }

  onInputTypeChange(newTypeId: string): void {
    const newType = this.props.paramTypes.find(ea => ea.id === newTypeId);
    this.props.onChange(this.props.input.clone({ paramType: newType }));
  }

  onQuestionChange(newQuestion: string): void {
    this.props.onChange(this.props.input.clone({ question: newQuestion }));
  }

  onSaveOptionChange(newOption: string): void {
    const changedProps = { isSavedForTeam: false, isSavedForUser: false };
    if (newOption === InputSaveOption.PER_TEAM) {
      changedProps.isSavedForTeam = true;
    } else if (newOption === InputSaveOption.PER_USER) {
      changedProps.isSavedForUser = true;
    }
    this.props.onChange(this.props.input.clone(changedProps));
  }

  onDeleteClick(): void {
    this.props.onDelete();
  }

  getCurrentParamTypeId(): Option<string> {
    return this.props.input.paramType ? this.props.input.paramType.id : null;
  }

  onConfigureType(): void {
    const paramTypeId = this.getCurrentParamTypeId();
    if (paramTypeId) {
      this.props.onConfigureType(paramTypeId);
    }
  }

  isConfigurable(): boolean {
    const pt = this.props.input.paramType;
    return Boolean(pt && pt.id !== pt.name);
  }

  focus(): void {
    if (this.nameInput) {
      this.nameInput.focus();
      this.nameInput.select();
    }
  }

  keyFor(paramType: ParamType): string {
    return 'param-type-' + this.props.id + '-' + paramType.id;
  }

  getInputSource() {
    let message: string;
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
  }

  getSaveOptionValue(): InputSaveOption {
    if (this.props.input.isSavedForTeam) {
      return InputSaveOption.PER_TEAM;
    } else if (this.props.input.isSavedForUser) {
      return InputSaveOption.PER_USER;
    } else {
      return InputSaveOption.EACH_TIME;
    }
  }

  getSavedAnswerCount(): number {
    return this.props.savedAnswers ? this.props.savedAnswers.userAnswerCount : 0;
  }

  getParamTypesOptions(): Array<SelectOption> {
    return this.props.paramTypes.map((ea) => ({
      key: this.keyFor(ea),
      value: ea.id || "",
      label: ea.name
    })).filter((ea) => Boolean(ea.value));
  }

  inputSavesAnswers(): boolean {
    return this.props.input.isSavedForTeam ||
        this.props.input.isSavedForUser;
  }

  getSavedAnswerSummary(): string {
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
  }

  onToggleSavedAnswer(): void {
    const inputId = this.props.input.inputId;
    if (inputId) {
      this.props.onToggleSavedAnswer(inputId);
    }
  }

  renderSavedAnswerInfo() {
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
    } else {
      return null;
    }
  }

  renderSharingInfo() {
    if (this.props.isShared) {
      return (
        <div className="box-tip mtneg1 mbneg1 phs border-left border-right">
          <span className="display-inline-block align-b type-green mrs" style={{ height: 24 }}>
            <SVGTip />
          </span>
          <span className="type-s mrm">This input is shared with other actions.</span>
        </div>
      );
    } else {
      return null;
    }
  }

  render() {
    const paramTypeId = this.getCurrentParamTypeId();
    return (
      <div>
        <div className="border border-light bg-white plm pbxs">
          <div className="columns columns-elastic">
            <div className="column column-expand align-form-input">
              <span className="display-inline-block align-m type-s type-weak mrm">Collect</span>
              <FormInput
                ref={(el) => this.nameInput = el}
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
              value={this.props.input.question}
              onChange={this.onQuestionChange}
              onEnterKey={this.props.onEnterKey}
              className="form-input-borderless"
            />
          </div>
          <div className="prsymbol mts">
            <Select className="form-select-s form-select-light align-m mrm mbs" name="paramType" value={this.getSaveOptionValue()} onChange={this.onSaveOptionChange}>
              <option value={InputSaveOption.EACH_TIME}>
                Ask each time action is triggered
              </option>
              <option value={InputSaveOption.PER_TEAM}>
                Ask once for the team, then re-use the answer
              </option>
              <option value={InputSaveOption.PER_USER}>
                Ask once for each user, then re-use the answer
              </option>
            </Select>
            <span className="display-inline-block align-m type-s type-weak mrm mbs">and allow data type</span>
            {paramTypeId ? (
              <Select className="form-select-s form-select-light align-m mrm mbs" name="paramType" value={paramTypeId} onChange={this.onInputTypeChange}>
                {this.getParamTypesOptions().map(Select.optionFor)}
              </Select>
            ) : null}
            {this.isConfigurable() ? (
              <button type="button" className="button-s button-shrink mbs" onClick={this.onConfigureType}>Edit type…</button>
            ) : null}
          </div>
          {this.renderSavedAnswerInfo()}
        </div>
        {this.renderSharingInfo()}
      </div>
    );
  }
}

export default UserInputDefinition;
