define(function(require) {
  var React = require('react'),
    DeleteButton = require('../shared_ui/delete_button'),
    FormInput = require('../form/input'),
    ImmutableObjectUtils = require('../lib/immutable_object_utils'),
    ScheduledAction = require('../models/scheduled_action');

  return React.createClass({
    displayName: 'ScheduledItemTitle',
    propTypes: {
      scheduledAction: React.PropTypes.instanceOf(ScheduledAction).isRequired,
      onChangeTriggerText: React.PropTypes.func.isRequired,
      onChangeAction: React.PropTypes.func.isRequired
    },

    hasSkillName: function() {
      return typeof this.props.scheduledAction.behaviorGroupName === "string";
    },

    getSkillName: function() {
      return this.props.scheduledAction.behaviorGroupName || "";
    },

    getActionName: function() {
      return this.props.scheduledAction.behaviorName || "";
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

    onChangeActionName: function(newName) {
      this.props.onChangeAction(newName, this.getArguments());
    },

    onChangeArgument: function(index, newArg) {
      const newArgs = ImmutableObjectUtils.arrayWithNewElementAtIndex(this.getArguments(), newArg, index);
      this.props.onChangeAction(this.getActionName(), newArgs);
    },

    onDeleteArgument: function(index) {
      const newArgs = ImmutableObjectUtils.arrayRemoveElementAtIndex(this.getArguments(), index);
      this.props.onChangeAction(this.getActionName(), newArgs);
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
      this.props.onChangeAction(this.getActionName(), this.getArguments().concat({ name: "", value: "" }), () => {
        const lastIndex = this.getArguments().length - 1;
        const nameInput = this.refs[`argumentName${lastIndex}`];
        if (nameInput) {
          nameInput.focus();
        }
      });
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
      return (
        <div>
          <div className="mbl">
            <span className="align-button mrm type-s">Run the action named</span>
            <FormInput className="form-input-borderless width-10 mrm" value={this.getActionName()} onChange={this.onChangeActionName} />
            {this.hasSkillName() ? (
              <span className="align-button type-s">
                <span> in the </span>
                <span className="border phxs mhxs type-black bg-white">{this.getSkillName()}</span>
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
            <button type="button" className="button-s" onClick={this.addArgument}>Pre-fill input for the action</button>
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
