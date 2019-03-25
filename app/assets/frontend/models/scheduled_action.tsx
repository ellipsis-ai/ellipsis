import Recurrence, {RecurrenceJson} from './recurrence';
import DeepEqual from '../lib/deep_equal';
import {Timestamp} from "../lib/formatter";
import BehaviorGroup from "./behavior_group";

export interface ScheduledActionArgument {
  name: string,
  value: any
}

export interface ScheduledActionJson {
  id?: Option<string>,
  scheduleType: string,
  behaviorId?: Option<string>,
  behaviorGroupId?: Option<string>,
  trigger?: Option<string>,
  arguments: Array<ScheduledActionArgument>,
  recurrence: RecurrenceJson,
  firstRecurrence?: Option<Timestamp>,
  secondRecurrence?: Option<Timestamp>,
  useDM: boolean,
  channel: string,
  userId?: Option<string>
}

export interface ScheduledActionInterface extends ScheduledActionJson {
  recurrence: Recurrence,
  firstRecurrence?: Option<Date>;
  secondRecurrence?: Option<Date>;
}

class ScheduledAction implements ScheduledActionInterface {
  readonly id: Option<string>;
  readonly scheduleType: string;
  readonly behaviorId: Option<string>;
  readonly behaviorGroupId: Option<string>;
  readonly trigger: Option<string>;
  readonly arguments: Array<ScheduledActionArgument>;
  readonly recurrence: Recurrence;
  readonly firstRecurrence: Option<Date>;
  readonly secondRecurrence: Option<Date>;
  readonly useDM: boolean;
  readonly channel: string;
  readonly userId: Option<string>;

    constructor(props: Partial<ScheduledActionInterface>) {
      const initialProps: ScheduledActionInterface = Object.assign({
        id: null,
        scheduleType: null,
        behaviorId: null,
        behaviorGroupId: null,
        trigger: null,
        arguments: [],
        recurrence: null,
        firstRecurrence: null,
        secondRecurrence: null,
        useDM: false,
        channel: "",
        userId: null
      }, props);

      Object.defineProperties(this, {
        id: { value: initialProps.id, enumerable: true },
        scheduleType: { value: initialProps.scheduleType, enumerable: true },
        behaviorId: { value: initialProps.behaviorId, enumerable: true },
        behaviorGroupId: { value: initialProps.behaviorGroupId, enumerable: true },
        trigger: { value: initialProps.trigger, enumerable: true },
        arguments: { value: initialProps.arguments, enumerable: true },
        recurrence: { value: initialProps.recurrence, enumerable: true },
        firstRecurrence: { value: initialProps.firstRecurrence, enumerable: true },
        secondRecurrence: { value: initialProps.secondRecurrence, enumerable: true },
        useDM: { value: initialProps.useDM, enumerable: true },
        channel: { value: initialProps.channel, enumerable: true },
        userId: { value: initialProps.userId, enumerable: true }
      });
    }

    getSkillNameFromGroups(behaviorGroups: Array<BehaviorGroup>): string {
      let name = "";
      if (this.behaviorGroupId) {
        const group = behaviorGroups.find((ea) => ea.id === this.behaviorGroupId);
        if (group) {
          name = group.getName();
        }
      }
      return name;
    }

    getActionNameFromGroups(behaviorGroups: Array<BehaviorGroup>): string {
      let name = "";
      if (this.behaviorGroupId && this.behaviorId) {
        const group = behaviorGroups.find((ea) => ea.id === this.behaviorGroupId);
        if (group) {
          const behaviorVersion = group.behaviorVersions.find((ea) => ea.behaviorId === this.behaviorId);
          if (behaviorVersion) {
            name = behaviorVersion.getName();
          }
        }
      }
      return name;
    }

    isNew(): boolean {
      return !this.id;
    }

    forEqualityComparison() {
      return this.clone({
        recurrence: this.recurrence.forEqualityComparison()
      });
    }

    isIdenticalTo(otherAction: ScheduledAction): boolean {
      return DeepEqual.isEqual(this.forEqualityComparison(), otherAction.forEqualityComparison());
    }

    isValidForScheduleType(): boolean {
      if (this.scheduleType === "message") {
        return Boolean(this.trigger && this.trigger.length > 0);
      } else if (this.scheduleType === "behavior") {
        return Boolean(this.behaviorId && this.behaviorGroupId && this.behaviorId.length > 0 && this.behaviorGroupId.length > 0);
      } else {
        return false;
      }
    }

    hasValidRecurrence(): boolean {
      return this.recurrence.isValid();
    }

    hasValidChannel(): boolean {
      return this.channel.length > 0;
    }

    isValid(): boolean {
      return this.isValidForScheduleType() && this.hasValidChannel() && this.hasValidRecurrence();
    }

    clone(props: Partial<ScheduledActionInterface>): ScheduledAction {
      return new ScheduledAction(Object.assign({}, this, props));
    }

    static dateFromTimestamp(t: Option<Timestamp>): Option<Date> {
      if (typeof t === 'string') {
        return new Date(t);
      } else if (typeof t === 'number') {
        return new Date(t);
      } else if (t instanceof Date) {
        return t;
      } else {
        return null;
      }
    }

    static fromJson(props: ScheduledActionJson): ScheduledAction {
      const materializedProps = Object.assign(props, {
        recurrence: new Recurrence(props.recurrence),
        firstRecurrence: ScheduledAction.dateFromTimestamp(props.firstRecurrence),
        secondRecurrence: ScheduledAction.dateFromTimestamp(props.secondRecurrence)
      });
      return new ScheduledAction(materializedProps);
    }

    static newWithDefaults(timeZone: Option<string>, timeZoneName: Option<string>): ScheduledAction {
      return new ScheduledAction({
        scheduleType: "message",
        trigger: "",
        recurrence: new Recurrence({
          frequency: 1,
          timeZone: timeZone,
          timeZoneName: timeZoneName
        }).becomeDaily()
      });
    }
  }

export default ScheduledAction;
