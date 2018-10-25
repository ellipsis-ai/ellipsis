import * as React from 'react';
import HelpButton from '../help/help_button';
import Formatter from '../lib/formatter';
import Notifications from '../notifications/notifications';
import SectionHeading from '../shared_ui/section_heading';
import Trigger, {TriggerType} from '../models/trigger';
import ParamNotInFunctionNotificationData from "../models/notifications/param_not_in_function_notification_data";
import InvalidParamInTriggerNotificationData from "../models/notifications/invalid_param_in_trigger_notification_data";
import autobind from "../lib/autobind";
import MessageTriggerInput from "./message_trigger_input";
import ReactionTriggerInput from "./reaction_trigger_input";
import DropdownMenu, {DropdownMenuItem} from "../shared_ui/dropdown_menu";

interface Props {
  triggers: Array<Trigger>
  inputNames: Array<string>
  onToggleHelp: () => void
  helpVisible: boolean
  onTriggerAdd: (newTrigger: Trigger, callback?: () => void) => void
  onTriggerChange: (index: number, newTrigger: Trigger) => void
  onTriggerDelete: (index: number) => void
  onTriggerDropdownToggle: (dropdownName: string) => void
  onAddNewInput: (name: string) => void
  openDropdownName: string
}

class TriggerConfiguration extends React.Component<Props> {
    messageTriggerInputs: Array<Option<MessageTriggerInput>>;

    constructor(props) {
      super(props);
      autobind(this);
      this.messageTriggerInputs = [];
    }

    hasPrimaryTrigger(): boolean {
      return !!(this.props.triggers.length > 0 && this.props.triggers[0].text);
    }

    onMessageTriggerEnterKey(index: number) {
      if (index + 1 < this.getMessageSentTriggers().length) {
        this.focusOnMessageTriggerIndex(index + 1);
      } else if (this.props.triggers[index].text) {
        this.addMessageTrigger();
      }
    }

    focusOnMessageTriggerIndex(index: number) {
      const input = this.messageTriggerInputs[index];
      if (input) {
        input.focus();
      }
    }

    focusOnFirstBlankTrigger(): void {
      const blankTriggerInput = this.messageTriggerInputs.find((maybeInput) => {
        return Boolean(maybeInput && maybeInput.isEmpty());
      });
      if (blankTriggerInput) {
        blankTriggerInput.focus();
      }
    }

    addTriggerType(triggerType: TriggerType): void {
      const newTrigger = new Trigger(triggerType);
      this.props.onTriggerAdd(newTrigger, () => {
        this.focusOnFirstBlankTrigger();
      });
    }

    addMessageTrigger(): void {
      this.addTriggerType("MessageSent");
    }

    addReactionTrigger(): void {
      this.addTriggerType("ReactionAdded");
    }

    onChangeHandlerForTrigger(trigger: Trigger): (newTrigger: Trigger) => void {
      return ((newTrigger: Trigger) => this.changeTrigger(trigger, newTrigger));
    }

    onDeleteHandlerForTrigger(trigger: Trigger): () => void {
      return (() => this.deleteTrigger(trigger));
    }

    changeTrigger(oldTrigger: Trigger, newTrigger: Trigger) {
      const index = this.props.triggers.findIndex((ea) => ea === oldTrigger);
      if (index >= 0) {
        this.props.onTriggerChange(index, newTrigger);
      }
    }

    deleteTrigger(trigger: Trigger) {
      const index = this.props.triggers.findIndex((ea) => ea === trigger);
      if (index >= 0) {
        this.props.onTriggerDelete(index);
      }
    }

    toggleDropdown(dropdownName: string) {
      this.props.onTriggerDropdownToggle(dropdownName);
    }

    getNotificationsFor(trigger: Trigger) {
      const unknownParamNames = trigger.paramNames().filter((ea) => !this.props.inputNames.includes(ea));
      return unknownParamNames.map((name) => {
        if (Formatter.isValidNameForCode(name)) {
          return new ParamNotInFunctionNotificationData({
            name: name,
            onClick: () => {
              this.props.onAddNewInput(name);
            }
          });
        } else {
          return new InvalidParamInTriggerNotificationData({
            name: name
          });
        }
      });
    }

    getMessageSentTriggers(): Array<Trigger> {
      return this.props.triggers.filter((ea) => ea.isMessageSentTrigger());
    }

    getReactionAddedTriggers(): Array<Trigger> {
      return this.props.triggers.filter((ea) => ea.isReactionAddedTrigger());
    }

    toggleAddTriggerDropdown(): void {
      this.toggleDropdown("addTrigger");
    }

    render() {
      const messageTriggers = this.getMessageSentTriggers();
      const reactionTriggers = this.getReactionAddedTriggers();
      return (
        <div className="columns container container-narrow ptxl">
          <div className="mbxxl">
            <SectionHeading number="1">
              <span>
                <span className="mrm">Triggers</span>
                <span className="display-inline-block">
                  <HelpButton onClick={this.props.onToggleHelp} toggled={this.props.helpVisible}/>
                </span>
              </span>
            </SectionHeading>
            <div className="mbm">
              {messageTriggers.length > 0 ? (
                <div>
                  <h5>Message triggers</h5>
                  {messageTriggers.map((trigger, index) => {
                    const notifications = this.getNotificationsFor(trigger);
                    const key = `messageTrigger${index}`;
                    return (
                      <div key={`${key}Container`}>
                        <MessageTriggerInput
                          id={`${key}Input`}
                          ref={(el) => this.messageTriggerInputs[index] = el}
                          trigger={trigger}
                          onChange={this.onChangeHandlerForTrigger(trigger)}
                          onDelete={this.onDeleteHandlerForTrigger(trigger)}
                          onEnterKey={() => this.onMessageTriggerEnterKey(index)}
                          onHelpClick={this.props.onToggleHelp}
                          helpVisible={this.props.helpVisible}
                        />
                        <div className={notifications.length > 0 ? "mtneg1 mbxs" : ""}>
                          <Notifications notifications={notifications} inline={true}/>
                        </div>
                        {index + 1 < messageTriggers.length ? (
                          <div className="pvxs type-label type-disabled align-c">or</div>
                        ) : null}
                      </div>
                    );
                  })}
                </div>
              ) : null}

              {reactionTriggers.length > 0 ? (
                <div>
                  <h5>Reaction triggers</h5>
                  <div>
                  {reactionTriggers.map((trigger, index) => {
                    const key = `reactionTrigger${index}`;
                    return (
                      <ReactionTriggerInput
                        key={`${key}Input`}
                        trigger={trigger}
                        id={`${key}Input`}
                        onChange={this.onChangeHandlerForTrigger(trigger)}
                        onDelete={this.onDeleteHandlerForTrigger(trigger)}
                        isShowingEmojiPicker={this.props.openDropdownName === `${key}EmojiPicker`}
                        onToggleEmojiPicker={this.toggleDropdown.bind(this, `${key}EmojiPicker`)}
                      />
                    )
                  })}
                  </div>
                </div>
              ) : null}

              <div className="mtm">
                <DropdownMenu
                  label={"Add trigger"}
                  openWhen={this.props.openDropdownName === "addTrigger"}
                  toggle={this.toggleAddTriggerDropdown}
                  labelClassName={"button-s"}
                >
                  <DropdownMenuItem label={"When a message is sent"} onClick={this.addMessageTrigger} />
                  <DropdownMenuItem label={"When a reaction is added to a message"} onClick={this.addReactionTrigger} />
                </DropdownMenu>
              </div>
            </div>
          </div>
        </div>
      );
    }
}

export default TriggerConfiguration;
