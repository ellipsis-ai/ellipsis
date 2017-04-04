define(function() {
  class NotificationDataGroup {
    constructor(props) {
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
    concat(member) {
      return this.clone({
        members: this.members.concat(member)
      });
    }
    hide() {
      return this.clone({
        hidden: true
      });
    }
    clone(newProps) {
      return new NotificationDataGroup(Object.assign({}, this, newProps));
    }

    static groupByKind(notifications) {
      var kinds = {};
      notifications.forEach((ea) => {
        if (!kinds[ea.kind]) {
          kinds[ea.kind] = new NotificationDataGroup({
            kind: ea.kind,
            members: [ea]
          });
        } else {
          kinds[ea.kind].concat(ea);
        }
      });
      return Object.keys(kinds).map((ea) => kinds[ea]);
    }

    static hideOldAndAppendNew(oldGroups, newGroups) {
      const notificationsToHide = oldGroups.filter((oldGroup) => !oldGroup.hidden && !newGroups.some((newGroup) => newGroup.kind === oldGroup.kind));
      return notificationsToHide.map((ea) => ea.hide()).concat(newGroups);
    }
  }

  return NotificationDataGroup;
});
