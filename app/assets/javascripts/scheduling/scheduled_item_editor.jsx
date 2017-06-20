define(function(require) {
  var React = require('react'),
    DynamicLabelButton = require('../form/dynamic_label_button'),
    RecurrenceEditor = require('./recurrence_editor'),
    ScheduleChannelEditor = require('./schedule_channel_editor'),
    ScheduledItemConfig = require('./scheduled_item_config'),
    BehaviorGroup = require('../models/behavior_group'),
    ScheduledAction = require('../models/scheduled_action'),
    ScheduleChannel = require('../models/schedule_channel');

  return React.createClass({
    displayName: 'ScheduledItemEditor',
    propTypes: {
      scheduledAction: React.PropTypes.instanceOf(ScheduledAction),
      channelList: React.PropTypes.arrayOf(React.PropTypes.instanceOf(ScheduleChannel)).isRequired,
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)).isRequired,
      onChange: React.PropTypes.func.isRequired,
      onCancel: React.PropTypes.func.isRequired,
      onSave: React.PropTypes.func.isRequired,
      isSaving: React.PropTypes.bool.isRequired,
      onDelete: React.PropTypes.func.isRequired,
      hasChanges: React.PropTypes.bool.isRequired,
      error: React.PropTypes.string,
      teamTimeZone: React.PropTypes.string.isRequired,
      teamTimeZoneName: React.PropTypes.string.isRequired,
      slackUserId: React.PropTypes.string.isRequired
    },

    shouldRenderItem: function() {
      return Boolean(this.props.scheduledAction);
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

    updateAction: function(behaviorId, newArgs, callback) {
      this.props.onChange(this.props.scheduledAction.clone({
        behaviorId: behaviorId,
        arguments: newArgs
      }), callback);
    },

    updateChannel: function(channelId) {
      this.props.onChange(this.props.scheduledAction.clone({
        channel: channelId
      }));
    },

    updateUseDM: function(newValue) {
      this.props.onChange(this.props.scheduledAction.clone({
        useDM: newValue
      }));
    },

    cancel: function() {
      this.props.onCancel();
    },

    save: function() {
      this.props.onSave();
    },

    delete: function() {
      this.props.onDelete();
    },

    hasChanges: function() {
      return this.props.hasChanges;
    },

    hasActiveRequest: function() {
      return this.props.isSaving;
    },

    renderDetails: function() {
      return (
        <div className="columns">
          <div className="column column-one-quarter mobile-column-full">
            <h4 className="type-weak">{this.props.scheduledAction.isNew() ? "New schedule" : "Edit schedule"}</h4>
          </div>
          <div className="column column-three-quarters mobile-column-full plxxl">
            <div>
              <h5 className="mbs">What to do</h5>
              <ScheduledItemConfig
                scheduledAction={this.props.scheduledAction}
                behaviorGroups={this.props.behaviorGroups}
                onChangeTriggerText={this.updateTriggerText}
                onChangeAction={this.updateAction}
              />
            </div>
            <hr />
            <div>
              <h5 className="mbs">Where to do it</h5>
              <ScheduleChannelEditor
                scheduledAction={this.props.scheduledAction}
                channelList={this.props.channelList}
                onChangeChannel={this.updateChannel}
                onChangeUseDM={this.updateUseDM}
                slackUserId={this.props.slackUserId}
              />
            </div>
            <hr />
            <div>
              <h5 className="mbl">When to repeat</h5>
              <RecurrenceEditor
                onChange={this.updateRecurrence}
                recurrence={this.props.scheduledAction.recurrence}
                teamTimeZone={this.props.teamTimeZone}
                teamTimeZoneName={this.props.teamTimeZoneName}
              />
            </div>

            <div className="mtxxl mbxl">
              <DynamicLabelButton
                disabledWhen={!this.hasChanges() || this.hasActiveRequest() || !this.props.scheduledAction.isValid()}
                className="button-primary mbs mrs"
                onClick={this.save}
                labels={[
                  { text: "Save changes", displayWhen: !this.props.isSaving },
                  { text: "Saving…", displayWhen: this.props.isSaving }
                ]}
              />
              <button type="button" className="mbs mrs" onClick={this.cancel} disabled={this.hasActiveRequest()}>Cancel</button>
              {this.props.scheduledAction.isNew() ? null : (
                <button type="button"
                  className="mrs mbs"
                  disabled={this.props.isSaving}
                  onClick={this.delete}
                >Unschedule this</button>
              )}
              {this.props.error ? (
                <span className="fade-in">
                  <span className="align-button mbs mrm" />
                  <span className="align-button mbs type-pink type-bold type-italic"> {this.props.error}</span>
                </span>
              ) : null}
            </div>
          </div>
        </div>
      );
    },

    render: function() {
      return (
        <div className="box-action phn">
          <div className="container container-wide">
            {this.shouldRenderItem() ? this.renderDetails() : null}
          </div>
        </div>
      );
    }
  });
});
