define(function() {

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
        behaviorGroupName: { value: initialProps.behaviorName, enumerable: true },
        behaviorId: { value: initialProps.behaviorName, enumerable: true },
        behaviorGroupId: { value: initialProps.behaviorName, enumerable: true },
        trigger: { value: initialProps.behaviorName, enumerable: true },
        arguments: { value: initialProps.behaviorName, enumerable: true },
        recurrence: { value: initialProps.behaviorName, enumerable: true },
        firstRecurrence: { value: initialProps.behaviorName, enumerable: true },
        secondRecurrence: { value: initialProps.behaviorName, enumerable: true },
        useDM: { value: initialProps.behaviorName, enumerable: true },
        channel: { value: initialProps.behaviorName, enumerable: true }
      });
    }
  }

  return ScheduledAction;
});
