import * as React from 'react';
import Formatter from '../lib/formatter';
import BehaviorGroup from '../models/behavior_group';
import ScheduledAction from '../models/scheduled_action';
import ScheduledItemTitle from './scheduled_item_title';

const ScheduledItem = React.createClass({
    propTypes: {
      scheduledAction: React.PropTypes.instanceOf(ScheduledAction).isRequired,
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)).isRequired,
      onClick: React.PropTypes.func.isRequired,
      className: React.PropTypes.string
    },

    getRecurrenceSummary: function() {
      return this.props.scheduledAction.recurrence.displayString;
    },

    getFirstRecurrence: function() {
      return this.props.scheduledAction.firstRecurrence;
    },

    getSecondRecurrence: function() {
      return this.props.scheduledAction.secondRecurrence;
    },

    toggle: function() {
      this.props.onClick(this.props.scheduledAction);
    },

    render: function() {
      return (
        <button type="button" className="button-block width-full" onClick={this.toggle}>
          <div className={this.props.className}>
            <div className="columns columns-elastic">
              <div className="column column-expand">
                <div className={"type-s"}>
                  <span><ScheduledItemTitle scheduledAction={this.props.scheduledAction} behaviorGroups={this.props.behaviorGroups} /> </span>
                  <span>{this.getRecurrenceSummary()}</span>
                </div>
              </div>
              <div className="column column-shrink">
                <div className="display-ellipsis link type-s">Edit</div>
              </div>
            </div>
            <div className={"mtl"}>
              <div className={"type-label type-weak mbxs"}>Next two times:</div>
              <div className={"type-xs"}>
                <span className={"mrs"}>
                  {Formatter.formatTimestampLong(this.getFirstRecurrence())}
                </span>
                <span className={"mrs type-weak"}>·</span>
                <span>
                  {Formatter.formatTimestampLong(this.getSecondRecurrence())}
                </span>
              </div>
            </div>
          </div>
        </button>
      );
    }
  });

export default ScheduledItem;
