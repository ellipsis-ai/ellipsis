import * as React from 'react';
import HelpButton from '../help/help_button';
import Formatter from '../lib/formatter';
import Notifications from '../notifications/notifications';
import SectionHeading from '../shared_ui/section_heading';
import TriggerInput from './trigger_input';
import Trigger from '../models/trigger';
import TriggerType from '../models/trigger_type';
import ParamNotInFunctionNotificationData from "../models/notifications/param_not_in_function_notification_data";
import InvalidParamInTriggerNotificationData from "../models/notifications/invalid_param_in_trigger_notification_data";
import autobind from "../lib/autobind";

interface Props {
  triggerTypes: Array<TriggerType>
  triggers: Array<Trigger>
  inputNames: Array<string>
  onToggleHelp: () => void
  helpVisible: boolean
  onTriggerAdd: (callback?: () => void) => void
  onTriggerChange: (index: number, newTrigger: Trigger) => void
  onTriggerDelete: (index: number) => void
  onTriggerDropdownToggle: (dropdownName: string) => void
  onAddNewInput: (name: string) => void
  openDropdownName: string
}

class TriggerConfiguration extends React.Component<Props> {
    triggerInputs: Array<Option<TriggerInput>>;

    constructor(props) {
      super(props);
      autobind(this);
      this.triggerInputs = [];
    }

    hasPrimaryTrigger(): boolean {
      return !!(this.props.triggers.length > 0 && this.props.triggers[0].text);
    }

    onTriggerEnterKey(index: number) {
      if (index + 1 < this.props.triggers.length) {
        this.focusOnTriggerIndex(index + 1);
      } else if (this.props.triggers[index].text) {
        this.addTrigger();
      }
    }

    focusOnTriggerIndex(index: number) {
      const input = this.triggerInputs[index];
      if (input) {
        input.focus();
      }
    }

    focusOnFirstBlankTrigger(): void {
      const blankTriggerInput = this.triggerInputs.find((maybeInput) => {
        return Boolean(maybeInput && maybeInput.isEmpty());
      });
      if (blankTriggerInput) {
        blankTriggerInput.focus();
      }
    }

    addTrigger(): void {
      this.props.onTriggerAdd(this.focusOnFirstBlankTrigger);
    }

    changeTrigger(index: number, newTrigger: Trigger) {
      this.props.onTriggerChange(index, newTrigger);
    }

    deleteTrigger(index: number) {
      this.props.onTriggerDelete(index);
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

    render() {
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
              {this.props.triggers.map((trigger, index) => {
                const notifications = this.getNotificationsFor(trigger);
                return (
                  <div key={`trigger${index}`}>
                    <TriggerInput
                      key={`BehaviorEditorTrigger${index}`}
                      id={`trigger${index}`}
                      ref={(el) => this.triggerInputs[index] = el}
                      triggerTypes={this.props.triggerTypes}
                      trigger={trigger}
                      onChange={this.changeTrigger.bind(this, index)}
                      onDelete={this.deleteTrigger.bind(this, index)}
                      onEnterKey={this.onTriggerEnterKey.bind(this, index)}
                      onHelpClick={this.props.onToggleHelp}
                      helpVisible={this.props.helpVisible}
                    />
                    <div className={notifications.length > 0 ? "mtneg1 mbxs" : ""}>
                      <Notifications notifications={notifications} inline={true}/>
                    </div>
                    {index + 1 < this.props.triggers.length ? (
                      <div className="pvxs type-label type-disabled align-c">or</div>
                    ) : null}
                  </div>
                );
              }, this)}
              <div className="mtm">
                <button type="button" className="button-s" onClick={this.addTrigger}>Add a trigger</button>
              </div>
            </div>
          </div>
        </div>
      );
    }
}

export default TriggerConfiguration;
