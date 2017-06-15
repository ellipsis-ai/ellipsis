define(function(require) {
  var React = require('react'),
    DeleteButton = require('../shared_ui/delete_button'),
    Select = require('../form/select'),
    FormInput = require('../form/input'),
    ImmutableObjectUtils = require('../lib/immutable_object_utils'),
    BehaviorGroup = require('../models/behavior_group'),
    ScheduledAction = require('../models/scheduled_action'),
    Sort = require('../lib/sort');

  return React.createClass({
    displayName: 'ScheduledItemTitle',
    propTypes: {
      scheduledAction: React.PropTypes.instanceOf(ScheduledAction).isRequired,
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)).isRequired,
      onChangeTriggerText: React.PropTypes.func.isRequired,
      onChangeAction: React.PropTypes.func.isRequired
    },

    getActionId: function() {
      return this.props.scheduledAction.behaviorId;
    },

    getArguments: function() {
      return this.props.scheduledAction.arguments;
    },

    getTriggerText: function() {
      return this.props.scheduledAction.trigger || "";
    },

    hasTriggerText: function() {
      return typeof this.props.scheduledAction.trigger === "string";
    },

    onChangeTriggerText: function(newText) {
      this.props.onChangeTriggerText(newText);
    },

    onChangeAction: function(newId) {
      this.props.onChangeAction(newId, this.getArguments());
    },

    onChangeArgument: function(index, newArg) {
      const newArgs = ImmutableObjectUtils.arrayWithNewElementAtIndex(this.getArguments(), newArg, index);
      this.props.onChangeAction(this.getActionId(), newArgs);
    },

    onDeleteArgument: function(index) {
      const newArgs = ImmutableObjectUtils.arrayRemoveElementAtIndex(this.getArguments(), index);
      this.props.onChangeAction(this.getActionId(), newArgs);
    },

    onChangeArgumentName: function(index, newName) {
      const oldArg = this.getArguments()[index];
      this.onChangeArgument(index, { name: newName, value: oldArg.value });
    },

    onChangeArgumentValue: function(index, newValue) {
      const oldArg = this.getArguments()[index];
      this.onChangeArgument(index, { name: oldArg.name, value: newValue });
    },

    addArgument: function() {
      this.props.onChangeAction(this.getActionId(), this.getArguments().concat({ name: "", value: "" }), () => {
        const lastIndex = this.getArguments().length - 1;
        const nameInput = this.refs[`argumentName${lastIndex}`];
        if (nameInput) {
          nameInput.focus();
        }
      });
    },

    getActionOptions: function() {
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
    },

    renderTriggerConfig: function() {
      return (
        <div>
          <div className="type-s mbxs">Run any action triggered by the message:</div>
          <FormInput placeholder="Enter a message that would trigger a response"
            value={this.getTriggerText()}
            onChange={this.onChangeTriggerText}
          />
        </div>
      );
    },

    renderActionConfig: function() {
      const actions = this.getActionOptions();
      const skillName = this.props.scheduledAction.getSkillNameFromGroups(this.props.behaviorGroups);
      return (
        <div>
          <div className="mbl">
            <span className="align-button mrm type-s">Run the action named</span>
            <span className="align-button mrm height-xl">
              <Select
                className="form-select-s width-10"
                value={this.props.scheduledAction.behaviorId}
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
    },

    renderArguments: function() {
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
                  <FormInput ref={`argumentName${index}`} className="form-input-borderless" value={arg.name}
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
              <button type="button" className="button-s" onClick={this.addArgument}>Add another input</button>
            </div>
          </div>
        );
      } else {
        return (
          <div className="mtm">
            <button type="button" className="button-s" onClick={this.addArgument}>Provide input answers</button>
          </div>
        );
      }
    },

    render: function() {
      if (this.hasTriggerText()) {
        return this.renderTriggerConfig();
      } else {
        return this.renderActionConfig();
      }
    }
  });
});
