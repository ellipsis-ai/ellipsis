import Recurrence from './recurrence';
import DeepEqual from '../lib/deep_equal';

  class ScheduledAction {

    constructor(props) {
      const initialProps = Object.assign({
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
        channel: ""
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
        channel: { value: initialProps.channel, enumerable: true }
      });
    }

    getSkillNameFromGroups(behaviorGroups) {
      let name = "";
      if (this.behaviorGroupId) {
        const group = behaviorGroups.find((ea) => ea.id === this.behaviorGroupId);
        if (group) {
          name = group.getName();
        }
      }
      return name;
    }

    getActionNameFromGroups(behaviorGroups) {
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

    isNew() {
      return !this.id;
    }

    forEqualityComparison() {
      return this.clone({
        recurrence: this.recurrence ? this.recurrence.forEqualityComparison() : null
      });
    }

    isIdenticalTo(otherAction) {
      return DeepEqual.isEqual(this.forEqualityComparison(), otherAction.forEqualityComparison());
    }

    isValidForScheduleType() {
      if (this.scheduleType === "message") {
        return this.trigger.length > 0;
      } else if (this.scheduleType === "behavior") {
        return this.behaviorId.length > 0 && this.behaviorGroupId.length > 0;
      } else {
        return false;
      }
    }

    hasValidRecurrence() {
      return this.recurrence && this.recurrence.isValid();
    }

    hasValidChannel() {
      return this.channel.length > 0;
    }

    isValid() {
      return this.isValidForScheduleType() && this.hasValidChannel() && this.hasValidRecurrence();
    }

    clone(props) {
      return new ScheduledAction(Object.assign({}, this, props));
    }

    static fromJson(props) {
      const materializedProps = Object.assign(props, {
        recurrence: new Recurrence(props.recurrence),
        firstRecurrence: new Date(props.firstRecurrence),
        secondRecurrence: new Date(props.secondRecurrence)
      });
      return new ScheduledAction(materializedProps);
    }

    static newWithDefaults(timeZone, timeZoneName) {
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