define(function() {

  class ScheduleChannel {

    constructor(props) {
      const initialProps = Object.assign({
        id: null,
        name: null,
        context: null,
        members: [],
        isPublic: false,
        isArchived: false
      }, props);

      Object.defineProperties(this, {
        id: { value: initialProps.id, enumerable: true },
        name: { value: initialProps.name, enumerable: true },
        context: { value: initialProps.context, enumerable: true },
        members: { value: initialProps.members, enumerable: true },
        isPublic: { value: initialProps.isPublic, enumerable: true },
        isArchived: { value: initialProps.isArchived, enumerable: true }
      });
    }

    clone(props) {
      return new ScheduleChannel(Object.assign({}, this, props));
    }

    static fromJson(props) {
      return new ScheduleChannel(props);
    }
  }

  return ScheduleChannel;
});
