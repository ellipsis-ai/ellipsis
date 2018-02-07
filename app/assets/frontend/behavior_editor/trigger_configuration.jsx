import * as React from 'react';
import HelpButton from '../help/help_button';
import Formatter from '../lib/formatter';
import Notifications from '../notifications/notifications';
import NotificationData from '../models/notification_data';
import SectionHeading from '../shared_ui/section_heading';
import TriggerInput from './trigger_input';
import Trigger from '../models/trigger';

const TriggerConfiguration = React.createClass({
    propTypes: {
      triggers: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Trigger)).isRequired,
      inputNames: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
      onToggleHelp: React.PropTypes.func.isRequired,
      helpVisible: React.PropTypes.bool.isRequired,
      onTriggerAdd: React.PropTypes.func.isRequired,
      onTriggerChange: React.PropTypes.func.isRequired,
      onTriggerDelete: React.PropTypes.func.isRequired,
      onTriggerDropdownToggle: React.PropTypes.func.isRequired,
      onAddNewInput: React.PropTypes.func.isRequired,
      openDropdownName: React.PropTypes.string.isRequired
    },

    hasPrimaryTrigger: function() {
      return !!(this.props.triggers.length > 0 && this.props.triggers[0].text);
    },

    onTriggerEnterKey: function(index) {
      if (index + 1 < this.props.triggers.length) {
        this.focusOnTriggerIndex(index + 1);
      } else if (this.props.triggers[index].text) {
        this.addTrigger();
      }
    },

    focusOnTriggerIndex: function(index) {
      this.refs['trigger' + index].focus();
    },

    focusOnFirstBlankTrigger: function() {
      var blankTrigger = Object.keys(this.refs).find(function(key) {
        return key.match(/^trigger\d+$/) && this.refs[key].isEmpty();
      }, this);
      if (blankTrigger) {
        this.refs[blankTrigger].focus();
      }
    },

    addTrigger: function() {
      this.props.onTriggerAdd(this.focusOnFirstBlankTrigger);
    },

    changeTrigger: function(index, newTrigger) {
      this.props.onTriggerChange(index, newTrigger);
    },

    deleteTrigger: function(index) {
      this.props.onTriggerDelete(index);
    },

    toggleDropdown: function(dropdownName) {
      this.props.onTriggerDropdownToggle(dropdownName);
    },

    getNotificationsFor: function(trigger) {
      const unknownParamNames = trigger.paramNames().filter((ea) => !this.props.inputNames.includes(ea));
      return unknownParamNames.map((name) => {
        if (Formatter.isValidNameForCode(name)) {
          return new NotificationData({
            kind: "param_not_in_function",
            name: name,
            onClick: () => {
              this.props.onAddNewInput(name);
            }
          });
        } else {
          return new NotificationData({
            kind: "invalid_param_in_trigger",
            name: name
          });
        }
      });
    },

    render: function() {
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
              {this.props.triggers.map(function(trigger, index) {
                const notifications = this.getNotificationsFor(trigger);
                return (
                  <div key={`trigger${index}`}>
                    <TriggerInput
                      dropdownIsOpen={this.props.openDropdownName === `BehaviorEditorTriggerDropdown${index}`}
                      key={`BehaviorEditorTrigger${index}`}
                      id={`trigger${index}`}
                      ref={`trigger${index}`}
                      trigger={trigger}
                      onChange={this.changeTrigger.bind(this, index)}
                      onDelete={this.deleteTrigger.bind(this, index)}
                      onEnterKey={this.onTriggerEnterKey.bind(this, index)}
                      onHelpClick={this.props.onToggleHelp}
                      onToggleDropdown={this.toggleDropdown.bind(this, `BehaviorEditorTriggerDropdown${index}`)}
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
});

export default TriggerConfiguration;