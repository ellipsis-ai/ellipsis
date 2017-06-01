define(function(require) {

  const Recurrence = require('./recurrence');

  class ScheduledAction {

    constructor(props) {
      const initialProps = Object.assign({
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

    static fromJson(props) {
      const materializedProps = Object.assign(props, {
        recurrence: new Recurrence(props.recurrence)
      });
      return new ScheduledAction(materializedProps);
    }
  }

  return ScheduledAction;
});
