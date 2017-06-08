define(function(require) {
  var React = require('react'),
    RecurrenceEditor = require('./recurrence_editor'),
    ScheduledAction = require('../models/scheduled_action'),
    ScheduledItemConfig = require('./scheduled_item_config');

  return React.createClass({
    displayName: 'ScheduledItemEditor',
    propTypes: {
      scheduledAction: React.PropTypes.instanceOf(ScheduledAction),
      onChange: React.PropTypes.func.isRequired,
      onCancel: React.PropTypes.func.isRequired,
      teamTimeZone: React.PropTypes.string.isRequired
    },

    shouldRenderItem: function() {
      return !!this.props.scheduledAction;
    },

    updateRecurrence: function(newRecurrence) {
      this.props.onChange(this.props.scheduledAction.clone({
        recurrence: newRecurrence
      }));
    },

    updateTriggerText: function(newText) {
      this.props.onChange(this.props.scheduledAction.clone({
        trigger: newText
      }));
    },

    updateAction: function(behaviorName, newArgs, callback) {
      this.props.onChange(this.props.scheduledAction.clone({
        behaviorName: behaviorName,
        arguments: newArgs
      }), callback);
    },

    cancel: function() {
      this.props.onCancel();
    },

    renderDetails: function() {
      return (
        <div className="columns">
          <div className="column column-one-quarter mobile-column-full">
            <h4 className="type-weak">Edit schedule</h4>
          </div>
          <div className="column column-three-quarters mobile-column-full plxxl">
            <div>
              <h5 className="mbs">What to do</h5>
              <ScheduledItemConfig
                scheduledAction={this.props.scheduledAction}
                onChangeTriggerText={this.updateTriggerText}
                onChangeAction={this.updateAction}
              />
            </div>
            <hr />
            <div>
              <h5 className="mbl">When to repeat</h5>
              <RecurrenceEditor
                onChange={this.updateRecurrence}
                recurrence={this.props.scheduledAction.recurrence}
                teamTimeZone={this.props.teamTimeZone}
              />
            </div>

            <div className="mtxxl mbxl">
              <button type="button" className="button-primary" onClick={this.cancel}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      );
    },

    render: function() {
      return (
        <div className="box-action phn">
          <div className="container container-c">
            {this.shouldRenderItem() ? this.renderDetails() : null}
          </div>
        </div>
      );
    }
  });
});
