define(function(require) {
  var React = require('react'),
    RecurrenceEditor = require('./recurrence_editor'),
    ScheduleChannelEditor = require('./schedule_channel_editor'),
    ScheduledItemConfig = require('./scheduled_item_config'),
    ScheduledAction = require('../models/scheduled_action'),
    ScheduleChannel = require('../models/schedule_channel');

  return React.createClass({
    displayName: 'ScheduledItemEditor',
    propTypes: {
      scheduledAction: React.PropTypes.instanceOf(ScheduledAction),
      channelList: React.PropTypes.arrayOf(React.PropTypes.instanceOf(ScheduleChannel)).isRequired,
      onChange: React.PropTypes.func.isRequired,
      onCancel: React.PropTypes.func.isRequired,
      teamTimeZone: React.PropTypes.string.isRequired,
      slackUserId: React.PropTypes.string.isRequired
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

    updateChannel: function(channelId) {
      this.props.onChange(this.props.scheduledAction.clone({
        channel: channelId
      }));
    },

    cancel: function() {
      this.props.onCancel();
    },

    isNew: function() {
      return !this.props.scheduledAction.id;
    },

    renderDetails: function() {
      return (
        <div className="columns">
          <div className="column column-one-quarter mobile-column-full">
            <h4 className="type-weak">{this.isNew() ? "New schedule" : "Edit schedule"}</h4>
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
              />
            </div>

            <div className="mtxxl mbxl">
              <div className="columns columns-elastic mobile-columns-float">
                <div className="column column-expand">
                  <button type="button" className="button-primary mbs mrs" disabled={true}>Save changes</button>
                  <button type="button" className="mbs mrs" onClick={this.cancel}>Cancel</button>
                </div>
                <div className="column column-shrink align-r mobile-align-l">
                  {this.isNew() ? null : (
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
