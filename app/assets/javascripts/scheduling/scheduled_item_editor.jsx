define(function(require) {
  var React = require('react'),
    RecurrenceEditor = require('./recurrence_editor'),
    SectionHeading = require('../shared_ui/section_heading'),
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
      teamTimeZone: React.PropTypes.string.isRequired,
      teamTimeZoneName: React.PropTypes.string.isRequired,
      slackUserId: React.PropTypes.string.isRequired,
      slackBotUserId: React.PropTypes.string.isRequired
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

    updateChannel: function(channelId, useDM) {
      this.props.onChange(this.props.scheduledAction.clone({
        channel: channelId,
        useDM: useDM
      }));
    },

    renderDetails: function() {
      return (
        <div>
          <div className="pbxxxxl">
            <div className="container container-wide border-bottom pvxxl">
              <SectionHeading number="1">What to do</SectionHeading>
              <ScheduledItemConfig
                scheduledAction={this.props.scheduledAction}
                behaviorGroups={this.props.behaviorGroups}
                onChangeTriggerText={this.updateTriggerText}
                onChangeAction={this.updateAction}
              />
            </div>
            <div className="container container-wide border-bottom pvxxl">
              <SectionHeading number="2">Where to do it</SectionHeading>
              <ScheduleChannelEditor
                scheduledAction={this.props.scheduledAction}
                channelList={this.props.channelList}
                onChange={this.updateChannel}
                slackUserId={this.props.slackUserId}
                slackBotUserId={this.props.slackBotUserId}
              />
            </div>
            <div className="container container-wide pvxxl">
              <SectionHeading number="3">When to repeat</SectionHeading>
              <RecurrenceEditor
                onChange={this.updateRecurrence}
                recurrence={this.props.scheduledAction.recurrence}
                teamTimeZone={this.props.teamTimeZone}
                teamTimeZoneName={this.props.teamTimeZoneName}
              />
            </div>
          </div>
        </div>
      );
    },

    render: function() {
      if (this.shouldRenderItem()) {
        return this.renderDetails();
      } else {
        return null;
      }
    }
  });
});
