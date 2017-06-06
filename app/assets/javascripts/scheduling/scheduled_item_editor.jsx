define(function(require) {
  var React = require('react'),
    RecurrenceEditor = require('./recurrence_editor'),
    ScheduledAction = require('../models/scheduled_action'),
    ScheduledItemTitle = require('./scheduled_item_title');

  return React.createClass({
    displayName: 'ScheduledItemEditor',
    propTypes: {
      scheduledAction: React.PropTypes.instanceOf(ScheduledAction),
      onChange: React.PropTypes.func.isRequired,
      onCancel: React.PropTypes.func.isRequired
    },

    shouldRenderItem: function() {
      return !!this.props.scheduledAction;
    },

    updateRecurrence: function(newRecurrence) {
      this.props.onChange(this.props.scheduledAction.clone({
        recurrence: newRecurrence
      }));
    },

    cancel: function() {
      this.props.onCancel();
    },

    renderDetails: function() {
      return (
        <div className="columns">
          <div className="column column-one-quarter mobile-column-full">
            <ScheduledItemTitle scheduledAction={this.props.scheduledAction}/>
          </div>
          <div className="column column-three-quarters mobile-column-full plxxl">
            <div className="pbxl">
              <RecurrenceEditor
                onChange={this.updateRecurrence}
                recurrence={this.props.scheduledAction.recurrence}
              />
            </div>

            <div className="mvxl">
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
