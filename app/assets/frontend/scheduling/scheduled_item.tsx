import * as React from 'react';
import Formatter from '../lib/formatter';
import BehaviorGroup from '../models/behavior_group';
import ScheduledAction from '../models/scheduled_action';
import ScheduledItemTitle from './scheduled_item_title';
import autobind from "../lib/autobind";

interface Props {
  scheduledAction: ScheduledAction,
  behaviorGroups: Array<BehaviorGroup>,
  onClick: (scheduledAction: ScheduledAction) => void,
  className?: Option<string>
}

class ScheduledItem extends React.Component<Props> {
    constructor(props) {
      super(props);
      autobind(this);
    }

    getRecurrenceSummary(): string {
      return this.props.scheduledAction.recurrence.displayString || "";
    }

    toggle() {
      this.props.onClick(this.props.scheduledAction);
    }

    render() {
      const firstRecurrence = this.props.scheduledAction.firstRecurrence;
      const secondRecurrence = this.props.scheduledAction.secondRecurrence;
      return (
        <button type="button" className="button-block width-full" onClick={this.toggle}>
          <div className={this.props.className || ""}>
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
            {firstRecurrence ? (
              <div className={"mtl"}>
                <div className={"type-label type-weak mbxs"}>
                  {secondRecurrence ? "Next two times:" : "Next time:"}
                </div>
                <div className={"type-xs"}>
                  <span className={"mrs"}>{Formatter.formatTimestampLong(firstRecurrence)}</span>
                  {secondRecurrence ? (
                    <span>
                      <span className={"mrs type-weak"}>·</span>
                      <span>{Formatter.formatTimestampLong(secondRecurrence)}</span>
                    </span>
                  ) : null}
                </div>
              </div>
            ) : null}
          </div>
        </button>
      );
    }
}

export default ScheduledItem;