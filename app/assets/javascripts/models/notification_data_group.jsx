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
    concat(newMember) {
      return this.clone({
        members: this.members.concat(newMember)
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
        const group = kinds[ea.kind] || new NotificationDataGroup({ kind: ea.kind });
        kinds[ea.kind] = group.concat(ea);
      });
      return Object.keys(kinds).map((ea) => kinds[ea]);
    }

    static hideOldAndAppendNew(oldGroups, newGroups) {
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

  return NotificationDataGroup;
});
