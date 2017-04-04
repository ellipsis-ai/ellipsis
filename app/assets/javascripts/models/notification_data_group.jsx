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
      const merged = [];
      let brandNew = newGroups;
      oldGroups.forEach((oldGroup) => {
        if (oldGroup.hidden) {
          merged.push(oldGroup);
        } else {
          const replacement = newGroups.find((newGroup) => newGroup.kind === oldGroup.kind);
          if (replacement) {
            merged.push(replacement);
            brandNew = brandNew.filter((ea) => ea.kind !== replacement.kind);
          } else {
            merged.push(oldGroup.hide());
          }
        }
      });
      return merged.concat(brandNew);
    }
  }

  return NotificationDataGroup;
});
