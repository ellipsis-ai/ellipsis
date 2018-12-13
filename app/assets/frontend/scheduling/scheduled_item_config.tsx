import * as React from 'react';
import DeleteButton from '../shared_ui/delete_button';
import Select from '../form/select';
import FormInput from '../form/input';
import ImmutableObjectUtils from '../lib/immutable_object_utils';
import BehaviorGroup from '../models/behavior_group';
import ScheduledAction, {ScheduledActionArgument} from '../models/scheduled_action';
import Sort from '../lib/sort';
import autobind from "../lib/autobind";
import Button from "../form/button";

interface Props {
  scheduledAction: ScheduledAction
  behaviorGroups: Array<BehaviorGroup>
  onChangeTriggerText: (text: string) => void
  onChangeAction: (behaviorId: string, newArgs: Array<ScheduledActionArgument>, callback?: () => void) => void
}

class ScheduledItemTitle extends React.PureComponent<Props> {
    nameInputs: Array<Option<FormInput>>;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.nameInputs = [];
    }

    getActionId(): Option<string> {
      return this.props.scheduledAction.behaviorId;
    }

    getArguments(): Array<ScheduledActionArgument> {
      return this.props.scheduledAction.arguments;
    }

    getTriggerText(): string {
      return this.props.scheduledAction.trigger || "";
    }

    hasTriggerText(): boolean {
      return typeof this.props.scheduledAction.trigger === "string";
    }

    onChangeTriggerText(newText: string): void {
      this.props.onChangeTriggerText(newText);
    }

    onChangeAction(newId: string): void {
      this.props.onChangeAction(newId, this.getArguments());
    }

    onChangeArgument(index: number, newArg: ScheduledActionArgument): void {
      const newArgs = ImmutableObjectUtils.arrayWithNewElementAtIndex(this.getArguments(), newArg, index);
      const id = this.getActionId();
      if (id) {
        this.props.onChangeAction(id, newArgs);
      }
    }

    onDeleteArgument(index: number): void {
      const newArgs = ImmutableObjectUtils.arrayRemoveElementAtIndex(this.getArguments(), index);
      const id = this.getActionId();
      if (id) {
        this.props.onChangeAction(id, newArgs);
      }
    }

    onChangeArgumentName(index: number, newName: string): void {
      const oldArg = this.getArguments()[index];
      this.onChangeArgument(index, { name: newName, value: oldArg.value });
    }

    onChangeArgumentValue(index: number, newValue: string): void {
      const oldArg = this.getArguments()[index];
      this.onChangeArgument(index, { name: oldArg.name, value: newValue });
    }

    addArgument(): void {
      const id = this.getActionId();
      if (id) {
        this.props.onChangeAction(id, this.getArguments().concat({ name: "", value: "" }), () => {
          const lastIndex = this.getArguments().length - 1;
          const nameInput = this.nameInputs[lastIndex];
          if (nameInput) {
            nameInput.focus();
          }
        });
      }
    }

    getActionOptions(): Array<{ name: string, value: string }> {
      const group = this.props.behaviorGroups.find((ea) => ea.id === this.props.scheduledAction.behaviorGroupId);
      if (group) {
        const namedActions = group.getActions().filter((ea) => {
          return ea.behaviorId && ea.getName().length > 0;
        });
        const options = namedActions.map((ea) => {
          return {
            name: ea.getName(),
            value: ea.behaviorId
          };
        });
        return Sort.arrayAlphabeticalBy(options, (ea) => ea.name);
      } else {
        return [];
      }
    }

    renderTriggerConfig() {
      return (
        <div>
          <div className="type-s mbxs">Run any action triggered by the message:</div>
          <FormInput placeholder="Enter a message that would trigger a response"
            value={this.getTriggerText()}
            onChange={this.onChangeTriggerText}
          />
        </div>
      );
    }

    renderActionConfig() {
      const actions = this.getActionOptions();
      const skillName = this.props.scheduledAction.getSkillNameFromGroups(this.props.behaviorGroups);
      return (
        <div>
          <div className="mbl">
            <span className="align-button mrm type-s">Run the action named</span>
            <span className="align-button mrm height-xl">
              <Select
                className="form-select-s width-10"
                value={this.props.scheduledAction.behaviorId || ""}
                onChange={this.onChangeAction}
              >
                {actions.map((ea) => (
                  <option key={ea.value} value={ea.value}>{ea.name}</option>
                ))}
              </Select>
            </span>
            {skillName ? (
              <span className="align-button type-s">
                <span> in the </span>
                <span className="border phxs mhxs type-black bg-white">{skillName}</span>
                <span> skill</span>
              </span>
            ) : null}
          </div>
          <div className="mtl">
            {this.renderArguments()}
          </div>
        </div>
      );
    }

    renderArguments() {
      const args = this.getArguments();
      if (args.length > 0) {
        return (
          <div>
            <div className="type-s mbm">Include pre-filled input:</div>
            <div className="columns">
              <div className="column column-one-third type-label">Name of input</div>
              <div className="column column-two-thirds type-label">Answer to include</div>
            </div>
            {args.map((arg, index) => (
              <div className="columns" key={`argument${index}`}>
                <div className="column column-one-third">
                  <FormInput ref={(el) => this.nameInputs[index] = el} className="form-input-borderless" value={arg.name}
                    onChange={this.onChangeArgumentName.bind(this, index)}/>
                </div>
                <div className="column column-two-thirds">
                  <div className="columns columns-elastic">
                    <div className="column column-expand">
                      <FormInput className="form-input-borderless" value={arg.value}
                        onChange={this.onChangeArgumentValue.bind(this, index)}/>
                    </div>
                    <div className="column column-shrink">
                      <DeleteButton
                        onClick={this.onDeleteArgument.bind(this, index)}
                        title="Delete input value"
                      />
                    </div>
                  </div>
                </div>
              </div>
            ))}
            <div className="mtm">
              <Button className="button-s" onClick={this.addArgument}>Add another input</Button>
            </div>
          </div>
        );
      } else {
        return (
          <div className="mtm">
            <Button className="button-s" onClick={this.addArgument}>Provide input answers</Button>
          </div>
        );
      }
    }

    render() {
      if (this.hasTriggerText()) {
        return this.renderTriggerConfig();
      } else {
        return this.renderActionConfig();
      }
    }
}

export default ScheduledItemTitle;
