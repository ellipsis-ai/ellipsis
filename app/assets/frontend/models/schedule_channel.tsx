export interface ScheduleChannelJson {
  id: string,
  name: string,
  context: string,
  members: Array<string>,
  isPublic: boolean,
  isArchived: boolean
}

export interface ScheduleChannelInterface extends ScheduleChannelJson {}

class ScheduleChannel implements ScheduleChannelInterface {
  readonly id: string;
  readonly name: string;
  readonly context: string;
  readonly members: Array<string>;
  readonly isPublic: boolean;
  readonly isArchived: boolean;

    constructor(props: ScheduleChannelInterface) {
      Object.defineProperties(this, {
        id: { value: props.id, enumerable: true },
        name: { value: props.name, enumerable: true },
        context: { value: props.context, enumerable: true },
        members: { value: props.members, enumerable: true },
        isPublic: { value: props.isPublic, enumerable: true },
        isArchived: { value: props.isArchived, enumerable: true }
      });
    }

    getPrefix(): string {
      return this.isPublic ? "#" : "🔒 ";
    }

    getSuffix(): string {
      return this.isPrivateGroup() ? "(private)" : "";
    }

    getNameForUserId(userId: string) {
      if (this.isDM()) {
        if (this.members.includes(userId)) {
          return "Direct message to you";
        } else {
          return "Direct message to someone else"
        }
      } else {
        return this.name;
      }
    }

    getName(userId: string, options?: {
      formatting?: boolean
    }): string {
      const shouldFormat = options && options.formatting;
      const name = this.getNameForUserId(userId);
      return shouldFormat ? `${this.getPrefix()}${name} ${this.getSuffix()}`.trim() : name;
    }

    isDM(): boolean {
      return !this.isPublic && this.members.length <= 2;
    }

    isPrivateGroup(): boolean {
      return !this.isPublic && this.members.length > 2;
    }

    userCanAccess(slackUserId: string): boolean {
      return this.isPublic || this.members.includes(slackUserId);
    }

    getFormattedName(userId: string): string {
      return this.getName(userId, { formatting: true });
    }

    getDescription(userId: string): string {
      if (this.isDM()) {
        if (this.members.includes(userId)) {
          return "a direct message to you";
        } else {
          return "a direct message to someone else";
        }
      } else if (this.isPrivateGroup()) {
        return `the private group ${this.getFormattedName(userId)}`;
      } else {
        return `the channel ${this.getFormattedName(userId)}`;
      }
    }

    clone(props: Partial<ScheduleChannelInterface>): ScheduleChannel {
      return new ScheduleChannel(Object.assign({}, this, props));
    }

    static fromJson(props: ScheduleChannelJson): ScheduleChannel {
      return new ScheduleChannel(props);
    }
}

export default ScheduleChannel;
