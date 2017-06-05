define(function(require) {
  var React = require('react'),
    ScheduledAction = require('../models/scheduled_action');

  return React.createClass({
    displayName: 'ScheduledItemEditor',
    propTypes: {
      scheduledAction: React.PropTypes.instanceOf(ScheduledAction)
    },

    renderDetails: function() {
      return (
        <div className="pvxl">{this.props.scheduledAction.recurrence.displayString}</div>
      );
    },

    render: function() {
      return (
        <div className="box-action phn">
          <div className="container container-c">
            {this.props.scheduledAction ? this.renderDetails() : null}
          </div>
        </div>
      );
    }
  });
});
