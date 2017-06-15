define(function(require) {
  var React = require('react'),
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

    cancel: function() {
      this.props.onCancel();
    },

    save: function() {
      this.props.onSave();
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
                onChange={this.updateChannel}
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
              <div className="columns columns-elastic mobile-columns-float">
                <div className="column column-expand">
                  <button type="button" className="button-primary mbs mrs" disabled={false} onClick={this.save}>Save changes</button>
                  <button type="button" className="mbs mrs" onClick={this.cancel}>Cancel</button>
                </div>
                <div className="column column-shrink align-r mobile-align-l">
                  {this.props.scheduledAction.isNew() ? null : (
                    <button type="button" className="mbs button-shrink" disabled={true}>Unschedule this item</button>
                  )}
                </div>
              </div>
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
