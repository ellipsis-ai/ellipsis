import * as React from 'react';
import Formatter from '../lib/formatter';
import BehaviorGroup from '../models/behavior_group';
import ScheduledAction from '../models/scheduled_action';
import ScheduledItemTitle from './scheduled_item_title';
import autobind from "../lib/autobind";
import {ValidTriggerInterface} from "./loader";
import SVGInfo from "../svg/info";
import SVGWarning from "../svg/warning";

interface Props {
  scheduledAction: ScheduledAction,
  behaviorGroups: Array<BehaviorGroup>,
  onClick: (scheduledAction: ScheduledAction) => void,
  className?: Option<string>
  userTimeZone: Option<string>
  userTimeZoneName: Option<string>
  validTrigger: Option<ValidTriggerInterface>
}

class ScheduledItem extends React.Component<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    getRecurrenceSummary(): string {
      return this.props.scheduledAction.recurrence.displayString || "";
    }

    toggle(): void {
      this.props.onClick(this.props.scheduledAction);
    }

    shouldShowUserTimeZone(): boolean {
      const hasUserZoneName = Boolean(this.props.userTimeZoneName);
      const isUserZone = this.props.scheduledAction.recurrence.timeZone === this.props.userTimeZone;
      const isUserZoneName = this.props.scheduledAction.recurrence.timeZoneName === this.props.userTimeZoneName;
      return hasUserZoneName && (!isUserZone && !isUserZoneName);
    }

    renderWarning() {
      const actionCount = this.props.validTrigger && this.props.validTrigger.matchingBehaviorIds.length;
      if (actionCount === 0) {
        return (
          <div className="mtl type-s fade-in">
            <span className="type-pink display-inline-block height-xl align-m mrs">
              <SVGWarning />
            </span>
            <span className="type-italic type-weak">Warning: this scheduled message will not trigger any known action.</span>
          </div>
        );
      } else if (typeof actionCount === "number" && actionCount > 1) {
        return (
          <div className="mtl type-s fade-in">
            <span className="type-yellow display-inline-block height-xl align-m mrs">
              <SVGInfo />
            </span>
            <span className="type-italic type-weak">This scheduled message will trigger {actionCount} actions.</span>
          </div>
        );
      } else {
        return null;
      }
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
                  <span>{secondRecurrence ? "Next two times" : "Next time"}</span>
                  <span>{this.shouldShowUserTimeZone() ? ` (${this.props.userTimeZoneName})` : ""}</span>
                  <span>:</span>
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
            {this.renderWarning()}
          </div>
        </button>
      );
    }
}

export default ScheduledItem;
