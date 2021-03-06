import * as React from 'react';
import DeleteButton from '../shared_ui/delete_button';
import Select from '../form/select';
import FormInput, {FocusableTextInputInterface} from '../form/input';
import ImmutableObjectUtils from '../lib/immutable_object_utils';
import BehaviorGroup from '../models/behavior_group';
import ScheduledAction, {ScheduledActionArgument} from '../models/scheduled_action';
import Sort from '../lib/sort';
import autobind from "../lib/autobind";
import Button from "../form/button";
import ToggleGroup, {ToggleGroupItem} from "../form/toggle_group";
import * as debounce from "javascript-debounce";
import {DataRequest} from "../lib/data_request";
import BehaviorVersion from "../models/behavior_version";
import SVGCheckmark from "../svg/checkmark";
import SVGInfo from "../svg/info";
import {ValidBehaviorIdTriggerJson, ValidTriggerJson} from "./data_layer";
import SVGWarning from "../svg/warning";
import Collapsible from "../shared_ui/collapsible";
import SVGExpand from "../svg/expand";
import {maybeDiffFor} from "../models/diffs";
import Trigger from "../models/trigger";
import Input from "../models/input";

interface Props {
  teamId: string,
  csrfToken: string,
  scheduledAction: ScheduledAction
  behaviorGroups: Array<BehaviorGroup>
  onChangeTriggerText: (text: string) => void
  onChangeAction: (behaviorId: string, newArgs: Array<ScheduledActionArgument>, callback?: () => void) => void
  onChangeSkill: (behaviorGroupId: string) => void
  onToggleByTrigger: (byTrigger: boolean) => void
  isForSingleGroup: boolean,
  groupId: Option<string>
}

interface MatchingGroupAndBehaviorVersion {
  group: BehaviorGroup
  behaviorVersion: BehaviorVersion
}

interface State {
  matchingBehaviorTriggers: Array<ValidBehaviorIdTriggerJson>
  loadingValidation: boolean
  validationError: Option<string>
  showPossibleTriggers: boolean
  selectArgumentNameValues: Array<Option<string>>
}

const SELECT_OTHER_INPUT = "--other--";

class ScheduledItemTitle extends React.PureComponent<Props, State> {
    nameInputs: Array<Option<FocusableTextInputInterface>>;
    valueInputs: Array<Option<FocusableTextInputInterface>>;
    triggerInput: Option<FormInput>;
    validateTrigger: (text: string) => void;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.nameInputs = [];
      this.valueInputs = [];
      this.validateTrigger = debounce(this._validateTrigger, 500);
      this.state = {
        matchingBehaviorTriggers: [],
        loadingValidation: Boolean(this.props.scheduledAction.trigger),
        validationError: null,
        showPossibleTriggers: false,
        selectArgumentNameValues: this.getSelectArgumentNameValues(props)
      };
    }

    componentDidMount(): void {
      this.beginValidatingTrigger(this.props);
      if (typeof this.props.scheduledAction.trigger === "string" && this.triggerInput) {
        this.triggerInput.focus();
      }
      window.addEventListener('focus', this.onFocus);
    }

    componentWillUnmount(): void {
      window.removeEventListener('focus', this.onFocus);
    }

    onFocus(): void {
      this.beginValidatingTrigger(this.props);
    }

    componentWillUpdate(nextProps: Readonly<Props>): void {
      if (nextProps.scheduledAction.id !== this.props.scheduledAction.id) {
        this.setState({
          matchingBehaviorTriggers: []
        });
      } else if (nextProps.scheduledAction.trigger !== this.props.scheduledAction.trigger) {
        this.beginValidatingTrigger(nextProps);
      } else if (nextProps.scheduledAction.behaviorId !== this.props.scheduledAction.behaviorId ||
        nextProps.scheduledAction.arguments.length !== this.props.scheduledAction.arguments.length) {
        this.setState({
          selectArgumentNameValues: this.getSelectArgumentNameValues(nextProps)
        });
      }
    }

    getSelectArgumentNameValues(props: Props): Array<string> {
      const args = this.getArguments(props);
      const inputs = this.getSelectedBehaviorInputs(props);
      const selectArgumentNameValues: Array<string> = [];
      args.forEach((arg, index) => {
        const input = inputs.find((input) => input.name === arg.name);
        if (arg.name && input) {
          selectArgumentNameValues[index] = arg.name;
        } else if (arg.name && !input) {
          selectArgumentNameValues[index] = SELECT_OTHER_INPUT;
        } else {
          selectArgumentNameValues[index] = "";
        }
      });
      return selectArgumentNameValues;
    }

    componentDidUpdate(prevProps: Readonly<Props>): void {
      if (typeof prevProps.scheduledAction.trigger !== "string" &&
        typeof this.props.scheduledAction.trigger === "string" && this.triggerInput) {
        this.triggerInput.focus();
      }
    }

    beginValidatingTrigger(props: Props): void {
      const text = props.scheduledAction.trigger;
      if (text) {
        this.setState({
          loadingValidation: true,
          matchingBehaviorTriggers: []
        }, () => {
          this.validateTrigger(text);
        });
      } else {
        this.setState({
          matchingBehaviorTriggers: []
        });
      }
    }

    _validateTrigger(text: string): void {
      DataRequest.jsonPost(jsRoutes.controllers.ScheduledActionsController.validateTriggers().url, {
        triggerMessages: [text],
        teamId: this.props.teamId
      }, this.props.csrfToken).then((results: Array<ValidTriggerJson>) => {
        if (this.props.scheduledAction.trigger === text) {
          const firstMatch = results.find((ea) => ea.text === text);
          this.setState({
            loadingValidation: false,
            matchingBehaviorTriggers: firstMatch ? firstMatch.matchingBehaviorTriggers : []
          });
        } else {
          // Throw away results if the user's trigger text has changed before request completes
          this.setState({
            loadingValidation: false
          });
        }
      }).catch(() => {
        this.setState({
          loadingValidation: false,
          validationError: "An error occurred while trying to check the trigger"
        });
      });
    }

    getActionId(): Option<string> {
      return this.props.scheduledAction.behaviorId;
    }

    getArguments(props?: Props): Array<ScheduledActionArgument> {
      return (props || this.props).scheduledAction.arguments;
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

    onChangeAction(newBehaviorId: string): void {
      this.props.onChangeAction(newBehaviorId, this.getArguments(this.props));
    }

    onChangeSkill(newGroupId: string): void {
      this.props.onChangeSkill(newGroupId);
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

    showFreeFormArgumentInputFor(index: number, numInputs: number): boolean {
      return numInputs === 0 || this.state.selectArgumentNameValues[index] === SELECT_OTHER_INPUT;
    }

    onSelectInput(index: number, value: string): void {
      this.setState({
        selectArgumentNameValues: ImmutableObjectUtils.arrayWithNewElementAtIndex(this.state.selectArgumentNameValues, value, index)
      });
      this.onChangeArgumentName(index, value === SELECT_OTHER_INPUT ? "" : value);
      const nameInput = this.nameInputs[index];
      const valueInput = this.valueInputs[index];
      if (value === SELECT_OTHER_INPUT && nameInput) {
        nameInput.focus();
      } else if (value && valueInput) {
        valueInput.focus();
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

    getSkillOptions(): Array<{ name: string, value: string }> {
      return [{
        name: "Select a skill…",
        value: ""
      }].concat(this.props.behaviorGroups.filter((ea) => Boolean(ea.id)).map((ea) => {
        return {
          name: ea.getName(),
          value: ea.id as string
        };
      }));
    }

    getActionOptions(): Array<{ name: string, value: string }> {
      const group = this.props.scheduledAction.behaviorGroupId ?
        this.props.behaviorGroups.find((ea) => ea.id === this.props.scheduledAction.behaviorGroupId) : null;
      const firstAction = {
        name: "Select an action…",
        value: ""
      };
      if (group) {
        const namedActions = group.getActions().filter((ea) => {
          return ea.behaviorId && ea.getName().length > 0;
        });
        const actionOptions = namedActions.map((ea) => {
          return {
            name: ea.getName(),
            value: ea.behaviorId
          };
        });
        return [firstAction].concat(Sort.arrayAlphabeticalBy(actionOptions, (ea) => ea.name));
      } else {
        return [firstAction];
      }
    }

    selectScheduleByTrigger(): void {
      this.props.onToggleByTrigger(true);
    }

    selectScheduleByAction(): void {
      this.props.onToggleByTrigger(false);
    }

    getMatchingGroupAndBehavior(desiredBehaviorId: string): Option<MatchingGroupAndBehaviorVersion> {
      let matchingBehavior: Option<BehaviorVersion>;
      const matchingGroup = this.props.behaviorGroups.find((group) => {
        return group.behaviorVersions.some((behaviorVersion) => {
          if (behaviorVersion.behaviorId === desiredBehaviorId) {
            matchingBehavior = behaviorVersion;
            return true;
          } else {
            return false;
          }
        });
      });
      if (matchingGroup && matchingBehavior) {
        return {
          group: matchingGroup,
          behaviorVersion: matchingBehavior
        };
      } else {
        return null;
      }
    }

    getMatchingActions() {
      const behaviorCount = this.state.matchingBehaviorTriggers.length;
      const oneValidBehavior = behaviorCount === 1;
      return this.state.matchingBehaviorTriggers.map((behaviorTriggers, matchingTriggerIndex) => {
        const matching = this.getMatchingGroupAndBehavior(behaviorTriggers.behaviorId);
        if (matching) {
          const group = matching.group;
          const behaviorVersion = matching.behaviorVersion;
          const triggerCount = behaviorTriggers.triggers.length;
          const oneValidTrigger = triggerCount === 1;
          return (
            <div key={`behaviorId${behaviorVersion.behaviorId}`} className="mtxs columns columns-elastic">
              <div className="column column-shrink prs">
                {oneValidBehavior && oneValidTrigger ? (
                  <div className="height-xl type-green">
                    <SVGCheckmark />
                  </div>
                ) : (
                  <div className="height-xl type-yellow">
                    <SVGInfo />
                  </div>
                )}
              </div>
              <div className="column column-expand">
                <span>Will trigger action </span>
                <b className="border bg-white phxs mhxs">{behaviorVersion.getName()}</b>
                <span> in skill </span>
                <b className="border bg-white phxs mhxs">{group.getName()}</b>
                {this.renderEditLink(group.id, behaviorVersion.behaviorId)}
                {triggerCount > 1 ? (
                  <div className="type-s mtxs bg-white pvxs phs border border-yellow">
                    <div className="type-yellow type-bold type-italic">Warning: matches {triggerCount} different triggers:</div>
                    <div>
                      {behaviorTriggers.triggers.map((trigger, index) => (
                        <span
                          key={`behaviorVersion-${behaviorVersion.behaviorId}-trigger${index}`}
                          className="box-chat mrs"
                        >{trigger.text}</span>
                      ))}
                    </div>
                  </div>
                ) : null}
              </div>
            </div>
          );
        } else {
          return (
            <div key={`noMatch${matchingTriggerIndex}`} className="mtxs columns columns-elastic">
              <div className="column column-shrink prs">
                <div className="height-xl type-pink">
                  <SVGWarning />
                </div>
              </div>
              <div className="column column-expand">
                <span>Will trigger an action in a different skill</span>
              </div>
            </div>
          );
        }
      });
    }

    getSkillName(): string {
      const group = this.props.behaviorGroups.find((ea) => ea.id === this.props.scheduledAction.behaviorGroupId);
      return group ? group.getName() : "Unknown";
    }

    toggleShowPossibleTriggers(): void {
      this.setState({
        showPossibleTriggers: !this.state.showPossibleTriggers
      });
    }

    possibleTriggerMatches(behavior: BehaviorVersion, trigger: Trigger): boolean {
      return this.props.scheduledAction.trigger && trigger.isExactMatchFor(this.props.scheduledAction.trigger) ||
        this.state.matchingBehaviorTriggers.some((matchingBehavior) => {
          return matchingBehavior.behaviorId === behavior.behaviorId &&
            matchingBehavior.triggers.some((matchingTrigger) => {
              return !maybeDiffFor(trigger, Trigger.fromJson(matchingTrigger), null, false)
            });
        });
    }

    getSelectedGroup(optionalProps?: Props): Option<BehaviorGroup> {
      const props = optionalProps || this.props;
      if (props.isForSingleGroup) {
        return props.behaviorGroups[0];
      } else if (props.scheduledAction.behaviorGroupId) {
        return props.behaviorGroups.find((ea) => ea.id === props.scheduledAction.behaviorGroupId);
      } else {
        return null;
      }
    }

    getSelectedBehaviorFor(group: BehaviorGroup, props?: Props): Option<BehaviorVersion> {
      const behaviorId = (props || this.props).scheduledAction.behaviorId;
      return behaviorId ? group.getActions().find((bv) => bv.behaviorId === behaviorId) : null;
    }

    getActionsWithTriggersForGroup(group: BehaviorGroup): Array<BehaviorVersion> {
      return group.getActions().filter((ea) => ea.getRegularMessageTriggers().length > 0);
    }

    renderEditLink(groupId: Option<string>, behaviorId: Option<string>) {
      const editUrl = groupId ? jsRoutes.controllers.BehaviorEditorController.edit(groupId, behaviorId).url : null;
      return editUrl ? (
        <a href={editUrl} className="type-s mls" target={"editSkill"}>Edit</a>
      ) : null;
    }

    renderTriggerConfig() {
      const selectedGroup = this.getSelectedGroup();
      return (
        <div className="mtm">
          <div className="type-s mbxs">Run any action triggered by the message:</div>
          <div>
            <FormInput
              ref={(el) => this.triggerInput = el}
              placeholder="Enter a message that would trigger a response"
              value={this.getTriggerText()}
              onChange={this.onChangeTriggerText}
            />
          </div>
          {selectedGroup ? (
            <div className="type-s">
              <div className="link mts" onClick={this.toggleShowPossibleTriggers}>
                <span className="display-inline-block height-l align-m mrxs"><SVGExpand expanded={this.state.showPossibleTriggers} /></span>
                <span>Show all possible triggers</span>
              </div>
              <Collapsible revealWhen={this.state.showPossibleTriggers}>
                {this.getActionsWithTriggersForGroup(selectedGroup).map((action) => (
                  <div key={`possibleBehavior${action.behaviorId}`}>
                    {action.getRegularMessageTriggers().map((trigger) => (
                      <button
                        type="button"
                        key={`trigger${trigger.getIdForDiff()}`}
                        className="button-raw mvs mrxs"
                        onClick={this.onChangeTriggerText.bind(this, trigger.getText())}
                      >
                        <span className={`box-chat ${
                          this.possibleTriggerMatches(action, trigger) ? "box-chat-selected" : "box-chat-dark type-black"
                        }`}>{trigger.getText()}</span>
                      </button>
                    ))}
                  </div>
                ))}
              </Collapsible>
            </div>
          ) : null}
          <div>
            <h5>{this.state.matchingBehaviorTriggers.length <= 1 ? "Matching action" : "Matching actions"}</h5>
            {this.state.matchingBehaviorTriggers.length > 1 ? (
              <div className="type-yellow type-bold type-italic">
                This text will trigger {this.state.matchingBehaviorTriggers.length} actions to run at the same time.
              </div>
            ) : null}
            <div className="mtxs min-height-2-lines">
              {this.renderMatchingActions()}
            </div>
          </div>
        </div>
      );
    }

    renderMatchingActions() {
      if (this.state.loadingValidation) {
        return (
          <div className="pulse type-weak type-italic">Checking trigger text…</div>
        );
      } else if (this.state.matchingBehaviorTriggers.length > 0) {
        return (
          <div>{this.getMatchingActions()}</div>
        );
      } else if (!this.getTriggerText().trim()) {
        return (
          <div className="type-disabled">None</div>
        );
      } else if (this.state.validationError) {
        return (
          <div className="type-pink type-italic type-bold">{this.state.validationError}</div>
        )
      } else {
        return (
          <div className="type-pink type-italic type-bold">Warning: no actions will be triggered by this text.</div>
        )
      }
    }

    canModifyGroup(): boolean {
      return !this.props.isForSingleGroup && this.props.scheduledAction.isNew();
    }

    renderSkillSelector() {
      const skills = this.getSkillOptions();
      if (this.hasTriggerText() && !this.props.scheduledAction.isNew()) {
        return null;
      } else {
        return (
          <div>
            <span className="type-s mrm align-m display-inline-block">From the skill</span>
            {this.canModifyGroup() ? (
              <Select
                className="form-select-s width-10 align-b"
                value={this.props.scheduledAction.behaviorGroupId || ""}
                onChange={this.onChangeSkill}
              >
                {skills.map((ea) => (
                  <option key={ea.value} value={ea.value}>{ea.name}</option>
                ))}
              </Select>
            ) : (
              <span className="bg-white border phxs">{this.getSkillName()}</span>
            )}
          </div>
        )
      }
    }

    renderActionConfig() {
      const actions = this.getActionOptions();
      return (
        <div className="mtxs">
          <div className="mbl">
            <span className="align-button mrm type-s">Run action named</span>
            <span className="align-button mrm height-xl">
              <Select
                className="form-select-s width-10"
                value={this.props.scheduledAction.behaviorId || ""}
                onChange={this.onChangeAction}
                disabled={!this.props.scheduledAction.behaviorGroupId}
              >
                {actions.map((ea) => (
                  <option key={ea.value} value={ea.value}>{ea.name}</option>
                ))}
              </Select>
            </span>
            <span className="align-button height-xl">
              {this.renderEditLink(this.props.scheduledAction.behaviorGroupId, this.props.scheduledAction.behaviorId)}
            </span>
          </div>
          {this.renderArguments()}
        </div>
      );
    }

    getInputIntroFor(inputs: Array<Input>) {
      if (inputs.length === 1) {
        return (
          <div className="type-s type-bold mbs">This action has 1 input.</div>
        );
      } else if (inputs.length > 1) {
        return (
          <div className="type-s type-bold mbs">This action has {inputs.length} inputs.</div>
        );
      } else {
        return null;
      }
    }

    getSelectedBehaviorInputs(props?: Props): Array<Input> {
      const group = this.getSelectedGroup(props);
      const behavior = group ? this.getSelectedBehaviorFor(group, props) : null;
      return behavior && group ? group.getInputs().filter((input) => input.inputId && behavior.inputIds.includes(input.inputId)) : [];
    }

    renderArguments() {
      const args = this.getArguments();
      const inputs = this.getSelectedBehaviorInputs();
      return (
        <div className="mtl">
          {this.getInputIntroFor(inputs)}
          {args.length > 0 ? (
            <div>
              <div className="type-s mbm">Include pre-filled answers:</div>
              {args.map((ea, index) => this.renderArgument(ea, index, inputs))}
              <div className="mtm">
                <Button className="button-s" onClick={this.addArgument}>Add another answer</Button>
              </div>
            </div>
          ) : (
            <div>
              <Button className="button-s" onClick={this.addArgument}>Provide pre-filled answers</Button>
            </div>
          )}
        </div>
      );
    }

    getArgumentSelectorValueFor(index: number): string {
      return this.state.selectArgumentNameValues[index] || "";
    }

    renderArgument(arg: ScheduledActionArgument, index: number, inputs: Array<Input>) {
      const key = `argument-${index}`;
      const readOnly = !this.showFreeFormArgumentInputFor(index, inputs.length);
      const selectValue = this.getArgumentSelectorValueFor(index);
      const showFormInputs = Boolean(!inputs.length || selectValue);
      return (
        <div className="max-width-80 border mvm plm bg-white" key={key}>
          <div className="columns columns-elastic">
            <div className="column column-expand prm">
              <div className={`ptm`}>
                {inputs.length > 0 ? (
                  <div>
                    <span className="align-button align-button-s align-t mrxs">Input:</span>
                    <Select
                      value={selectValue}
                      onChange={this.onSelectInput.bind(this, index)}
                      className="mbm form-select-s align-b"
                    >
                      <option value="">Select question…</option>
                      {inputs.map((input, index) => (
                        <option key={`input-${input.inputId || index}`} value={input.name}>{input.question}</option>
                      ))}
                      <option value={SELECT_OTHER_INPUT}>Other…</option>
                    </Select>
                  </div>
                ) : null}
              </div>
              <Collapsible revealWhen={showFormInputs}>
                <div className="columns columns-elastic mbm">
                  <div className="column column-shrink prm">
                    <div className="width-10">
                      <FormInput ref={(el) => this.nameInputs[index] = el}
                        placeholder="Input name"
                        className={`form-input-borderless type-monospace ${
                          readOnly ? "type-bold border-transparent" : ""
                        }`}
                        value={arg.name}
                        onChange={this.onChangeArgumentName.bind(this, index)}
                        readOnly={readOnly}
                      />
                    </div>
                  </div>
                  <div className="column column-expand">
                    <FormInput
                      ref={(el) => this.valueInputs[index] = el}
                      placeholder="Answer text"
                      className="form-input-borderless" value={arg.value}
                      onChange={this.onChangeArgumentValue.bind(this, index)}
                    />
                  </div>
                </div>
              </Collapsible>
            </div>
            <div className="column column-shrink">
              <DeleteButton
                onClick={this.onDeleteArgument.bind(this, index)}
                title="Delete answer"
              />
            </div>
          </div>
        </div>
      )
    }

    renderforScheduleType() {
      if (this.hasTriggerText()) {
        return this.renderTriggerConfig();
      } else {
        return this.renderActionConfig();
      }
    }

    renderScheduleTypeToggle() {
      if (this.props.scheduledAction.isNew()) {
        return (
          <div className="mbm">
            <ToggleGroup className={"form-toggle-group-s"}>
              <ToggleGroupItem activeWhen={this.hasTriggerText()} label={"Schedule action by message"}
                onClick={this.selectScheduleByTrigger} />
              <ToggleGroupItem activeWhen={!this.hasTriggerText()} label={"Schedule action by name"}
                onClick={this.selectScheduleByAction} />
            </ToggleGroup>
          </div>
        );
      } else {
        return null;
      }
    }

    render() {
      return (
        <div>
          {this.renderScheduleTypeToggle()}
          {this.renderSkillSelector()}
          {this.renderforScheduleType()}
        </div>
      );
    }

}

export default ScheduledItemTitle;
