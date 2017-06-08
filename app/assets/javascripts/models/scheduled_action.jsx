define(function(require) {

  const Recurrence = require('./recurrence');

  class ScheduledAction {

    constructor(props) {
      const initialProps = Object.assign({
        id: null,
        scheduleType: null,
        behaviorName: null,
        behaviorGroupName: null,
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
        behaviorName: { value: initialProps.behaviorName, enumerable: true },
        behaviorGroupName: { value: initialProps.behaviorGroupName, enumerable: true },
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

    static newWithDefaults(timeZone) {
      return new ScheduledAction({
        scheduleType: "daily",
        trigger: "",
        recurrence: new Recurrence({ timeZone: timeZone }).becomeDaily()
      });
    }
  }

  return ScheduledAction;
});
