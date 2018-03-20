import {default as NotificationData, NotificationKind} from "./notification_data";

export interface NotificationDataGroupInterface {
  kind: NotificationKind,
  members?: Array<NotificationData>,
  hidden?: boolean
}

class NotificationDataGroup<T extends NotificationData> implements NotificationDataGroupInterface {
  readonly kind: NotificationKind;
  readonly members: Array<T>;
  readonly hidden: boolean;

  constructor(props: NotificationDataGroupInterface) {
      if (!props.kind) {
        throw new Error("NotificationDataGroup must have a kind property");
      }
      Object.defineProperties(this, {
        kind: {
          value: props.kind,
          enumerable: true
        },
        members: {
          value: props.members || [],
          enumerable: true
        },
        hidden: {
          value: !!props.hidden,
          enumerable: true
        }
      });
  }

    concat(newMember: T): NotificationDataGroup<T> {
      return this.clone({
        members: this.members.concat(newMember)
      });
    }

    hide(): NotificationDataGroup<any> {
      return this.clone({
        hidden: true
      });
    }

    clone(newProps: Partial<NotificationDataGroupInterface>): NotificationDataGroup<T> {
      return new NotificationDataGroup(Object.assign({}, this, newProps));
    }

    static groupByKind(notifications: Array<NotificationData>): Array<NotificationDataGroup<any>> {
      var kinds = {};
      notifications.forEach((ea) => {
        const group = kinds[ea.kind] || new NotificationDataGroup({ kind: ea.kind });
        kinds[ea.kind] = group.concat(ea);
      });
      return Object.keys(kinds).map((ea) => kinds[ea]);
    }

    static hideOldAndAppendNew(oldGroups: Array<NotificationDataGroup<any>>, newGroups: Array<NotificationDataGroup<any>>) {
      let brandNew = newGroups;
      const merged = oldGroups.map((oldGroup) => {
        if (oldGroup.hidden) {
          return oldGroup;
        } else {
          const replacement = newGroups.find((newGroup) => newGroup.kind === oldGroup.kind);
          if (replacement) {
            brandNew = brandNew.filter((ea) => ea.kind !== replacement.kind);
            return replacement;
          } else {
            return oldGroup.hide();
          }
        }
      });
      return merged.concat(brandNew);
    }
}

export default NotificationDataGroup;

