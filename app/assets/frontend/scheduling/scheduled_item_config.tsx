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
import ToggleGroup, {ToggleGroupItem} from "../form/toggle_group";
import * as debounce from "javascript-debounce";
import {DataRequest} from "../lib/data_request";
import {TriggerJson} from "../models/trigger";
import BehaviorVersion from "../models/behavior_version";
import SVGCheckmark from "../svg/checkmark";
import SVGInfo from "../svg/info";

interface Props {
  teamId: string,
  csrfToken: string,
  scheduledAction: ScheduledAction
  behaviorGroups: Array<BehaviorGroup>
  onChangeTriggerText: (text: string) => void
  onChangeAction: (behaviorId: string, newArgs: Array<ScheduledActionArgument>, callback?: () => void) => void
  onChangeSkill: (behaviorGroupId: string) => void
  onToggleByTrigger: (byTrigger: boolean) => void
}

interface ValidTriggerJson {
  trigger: TriggerJson
  behaviorId: string
}

interface MatchingGroupAndBehaviorVersion {
  group: BehaviorGroup
  behaviorVersion: BehaviorVersion
}

interface State {
  matchingValidTriggers: Array<ValidTriggerJson>
  loadingValidation: boolean
  validationError: Option<string>
}

class ScheduledItemTitle extends React.PureComponent<Props, State> {
    nameInputs: Array<Option<FormInput>>;
    triggerInput: Option<FormInput>;
    validateTrigger: (text: string) => void;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.nameInputs = [];
      this.validateTrigger = debounce(this._validateTrigger, 500);
      this.state = {
        matchingValidTriggers: [],
        loadingValidation: Boolean(this.props.scheduledAction.trigger),
        validationError: null
      };
    }

    componentDidMount(): void {
      if (this.props.scheduledAction.trigger) {
        this.beginValidatingTrigger(this.props.scheduledAction.trigger);
      } else if (typeof this.props.scheduledAction.trigger === "string" && this.triggerInput) {
        this.triggerInput.focus();
      }
    }

    componentDidUpdate(prevProps: Readonly<Props>): void {
      if (typeof prevProps.scheduledAction.trigger !== "string" &&
        typeof this.props.scheduledAction.trigger === "string" && this.triggerInput) {
        this.triggerInput.focus();
      }
      if (prevProps.scheduledAction.id !== this.props.scheduledAction.id) {
        this.setState({
          matchingValidTriggers: []
        });
      } else if (this.props.scheduledAction.trigger && prevProps.scheduledAction.trigger !== this.props.scheduledAction.trigger) {
        this.beginValidatingTrigger(this.props.scheduledAction.trigger);
      }
    }

    beginValidatingTrigger(text: string): void {
      this.setState({
        loadingValidation: true,
        matchingValidTriggers: []
      }, () => {
        this.validateTrigger(text);
      });
    }

    _validateTrigger(text: string): void {
      DataRequest.jsonPost(jsRoutes.controllers.ScheduledActionsController.validateTrigger().url, {
        text: text,
        teamId: this.props.teamId
      }, this.props.csrfToken).then((results: Array<ValidTriggerJson>) => {
        if (this.props.scheduledAction.trigger === text) {
          this.setState({
            loadingValidation: false,
            matchingValidTriggers: results
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

    onChangeAction(newBehaviorId: string): void {
      this.props.onChangeAction(newBehaviorId, this.getArguments());
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
      const oneValidTrigger = this.state.matchingValidTriggers.length === 1;
      return this.state.matchingValidTriggers.map((match) => {
        const matching = this.getMatchingGroupAndBehavior(match.behaviorId);
        if (matching) {
          const group = matching.group;
          const behaviorVersion = matching.behaviorVersion;
          return (
            <div key={`behaviorId${behaviorVersion.behaviorId}`} className="mvxs">
              {oneValidTrigger ? (
                <span className="display-inline-block height-xl type-green mrs align-m">
                  <SVGCheckmark />
                </span>
              ) : (
                <span className="display-inline-block height-xl type-pink mrs align-m">
                  <SVGInfo />
                </span>
              )}
              <span className="align-m">
                <span>Will trigger action </span>
                <b className="border bg-white phxs mhxs">{behaviorVersion.getName()}</b>
                <span> in skill </span>
                <b className="border bg-white phxs mhxs">{group.getName()}</b>
                {this.renderEditLink(group.id, behaviorVersion.behaviorId)}
              </span>
            </div>
          );
        } else {
          return null;
        }
      });
    }

    renderEditLink(groupId: Option<string>, behaviorId: Option<string>) {
      const editUrl = groupId ? jsRoutes.controllers.BehaviorEditorController.edit(groupId, behaviorId).url : null;
      return editUrl ? (
        <a href={editUrl} className="type-s mls" target={"editSkill"}>Edit</a>
      ) : null;
    }

    renderTriggerConfig() {
      return (
        <div>
          <div className="type-s mbxs">Run any action triggered by the message:</div>
          <div>
            <FormInput
              ref={(el) => this.triggerInput = el}
              placeholder="Enter a message that would trigger a response"
              value={this.getTriggerText()}
              onChange={this.onChangeTriggerText}
            />
          </div>
          <div>
            <h5>{this.state.matchingValidTriggers.length <= 1 ? "Matching action" : "Matching actions"}</h5>
            {this.state.matchingValidTriggers.length > 1 ? (
              <div className="type-pink type-bold type-italic">
                Warning: this text will trigger {this.state.matchingValidTriggers.length} actions to run at the same time.
              </div>
            ) : null}
            {this.renderMatchingActions()}
          </div>
        </div>
      );
    }

    renderMatchingActions() {
      if (this.state.loadingValidation) {
        return (
          <div className="pulse type-weak type-italic">Checking trigger text…</div>
        );
      } else if (this.state.matchingValidTriggers.length > 0) {
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

    renderActionConfig() {
      const actions = this.getActionOptions();
      const skills = this.getSkillOptions();
      return (
        <div>
          <div className="mbl">
            <span className="align-button mrm type-s">Run action from skill</span>
            <span className="align-button mrm height-xl">
              <Select
                className="form-select-s width-10"
                value={this.props.scheduledAction.behaviorGroupId || ""}
                onChange={this.onChangeSkill}
              >
                {skills.map((ea) => (
                  <option key={ea.value} value={ea.value}>{ea.name}</option>
                ))}
              </Select>
            </span>
            <span className="align-button mrm type-s">named</span>
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
          {this.renderforScheduleType()}
        </div>
      );
    }

}

export default ScheduledItemTitle;
