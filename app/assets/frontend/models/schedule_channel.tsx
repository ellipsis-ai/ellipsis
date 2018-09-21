export interface ScheduleChannelJson {
  id: string,
  name: string,
  context: string,
  isBotMember: boolean,
  isSelfDm: boolean,
  isOtherDm: boolean,
  isPrivateChannel: boolean,
  isPrivateGroup: boolean,
  isArchived: boolean,
  isShared: boolean,
  isReadOnly: boolean
}

export interface ScheduleChannelInterface extends ScheduleChannelJson {}

class ScheduleChannel implements ScheduleChannelInterface {
  readonly id: string;
  readonly name: string;
  readonly context: string;
  readonly isBotMember: boolean;
  readonly isSelfDm: boolean;
  readonly isOtherDm: boolean;
  readonly isPrivateChannel: boolean;
  readonly isPrivateGroup: boolean;
  readonly isArchived: boolean;
  readonly isShared: boolean;
  readonly isReadOnly: boolean;

    constructor(props: ScheduleChannelInterface) {
      Object.defineProperties(this, {
        id: { value: props.id, enumerable: true },
        name: { value: props.name, enumerable: true },
        context: { value: props.context, enumerable: true },
        isBotMember: { value: props.isBotMember, enumerable: true },
        isSelfDm: { value: props.isSelfDm, enumerable: true },
        isOtherDm: { value: props.isOtherDm, enumerable: true },
        isPrivateChannel: { value: props.isPrivateChannel, enumerable: true },
        isPrivateGroup: { value: props.isPrivateGroup, enumerable: true },
        isArchived: { value: props.isArchived, enumerable: true },
        isShared: { value: props.isShared, enumerable: true },
        isReadOnly: { value: props.isReadOnly, enumerable: true }
      });
    }

    isPublic(): boolean {
      return !this.isSelfDm && !this.isOtherDm && !this.isPrivateGroup && !this.isPrivateChannel;
    }

    isDm(): boolean {
      return this.isSelfDm || this.isOtherDm;
    }

    getPrefix(): string {
      return this.isPublic() ? "#" : "🔒 ";
    }

    getSuffix(): string {
      if (this.isShared) {
        return "(shared)";
      } else {
        return "";
      }
    }

    getUnformattedName() {
      if (this.isSelfDm) {
        return "Direct message to you";
      } else if (this.isOtherDm) {
        return "Direct message to someone else"
      } else {
        return this.name;
      }
    }

    getName(options?: {
      formatting?: boolean
    }): string {
      const shouldFormat = options && options.formatting;
      const name = this.getUnformattedName();
      return shouldFormat ? `${this.getPrefix()}${name} ${this.getSuffix()}`.trim() : name;
    }

    getFormattedName(): string {
      return this.getName({ formatting: true });
    }

    getDescription(): string {
      if (this.isSelfDm) {
        return "a direct message to you";
      } else if (this.isOtherDm) {
        return "a direct message to someone else";
      } else if (this.isPrivateGroup) {
        return `the private group ${this.getFormattedName()}`;
      } else {
        return `the channel ${this.getFormattedName()}`;
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
