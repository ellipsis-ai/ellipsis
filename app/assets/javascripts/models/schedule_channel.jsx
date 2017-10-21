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

    getPrefix() {
      return this.isPublic ? "#" : "🔒 ";
    }

    getSuffix() {
      return this.isPrivateGroup() ? "(private)" : "";
    }

    getName(options) {
      const shouldFormat = options && options.formatting;
      const name = this.isDM() ? "Direct message to you" : this.name;
      return shouldFormat ? `${this.getPrefix()}${name} ${this.getSuffix()}`.trim() : name;
    }

    isDM() {
      return !this.isPublic && this.members.length < 2;
    }

    isPrivateGroup() {
      return !this.isPublic && this.members.length > 1;
    }

    userCanAccess(slackUserId) {
      return this.isDM() || this.members.includes(slackUserId);
    }

    getFormattedName() {
      return this.getName({ formatting: true });
    }

    getDescription() {
      if (this.isDM()) {
        return "a direct message to you";
      } else if (this.isPrivateGroup()) {
        return `the private group ${this.getFormattedName()}`;
      } else {
        return `the channel ${this.getFormattedName()}`;
      }
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
