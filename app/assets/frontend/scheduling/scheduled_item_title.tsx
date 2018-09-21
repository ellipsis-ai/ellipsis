import * as React from 'react';
import BehaviorGroup from '../models/behavior_group';
import ScheduledAction from '../models/scheduled_action';
import autobind from "../lib/autobind";

interface Props {
  scheduledAction: ScheduledAction,
  behaviorGroups: Array<BehaviorGroup>
}

class ScheduledItemTitle extends React.PureComponent<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    getTriggerText(): string {
      return this.props.scheduledAction.trigger || "";
    }

    hasTriggerText(): boolean {
      return Boolean(this.props.scheduledAction.trigger);
    }

    renderTriggerTitle() {
      return (
        <span>
          <span>Run </span>
          <span className="box-chat mhs type-black">{this.getTriggerText()}</span>
        </span>
      );
    }

    renderActionNameTitle() {
      const skillName = this.props.scheduledAction.getSkillNameFromGroups(this.props.behaviorGroups);
      const actionName = this.props.scheduledAction.getActionNameFromGroups(this.props.behaviorGroups);
      return (
        <span className="">
          <span>Run </span>
          <span className="border phxs mhxs type-black bg-white">{actionName || "an unnamed action"}</span>
          {skillName ? (
            <span>
              <span> in the </span>
              <span className="border phxs mhxs type-black bg-white">{skillName}</span>
              <span> skill</span>
            </span>
          ) : null}
        </span>
      );
    }

    render() {
      if (this.hasTriggerText()) {
        return this.renderTriggerTitle();
      } else {
        return this.renderActionNameTitle();
      }
    }
}

export default ScheduledItemTitle;
