const NotificationData = require("../app/assets/javascripts/models/notification_data");
const NotificationDataGroup = require("../app/assets/javascripts/models/notification_data_group");

describe("NotificationDataGroup", () => {

  const foo1 = new NotificationData({ kind: "foo", message: "alpha" });
  const bar1 = new NotificationData({ kind: "bar", message: "bravo" });
  const foo2 = new NotificationData({ kind: "foo", message: "charlie" });
  const bar2 = new NotificationData({ kind: "bar", message: "delta" });

  describe("groupByKind", () => {
    it("assembles a list of NotificationData into an array of groups by kind", () => {
      const notifications = [foo1, bar1, foo2, bar2];
      expect(NotificationDataGroup.groupByKind(notifications)).toEqual([
        new NotificationDataGroup({ kind: "foo", members: [foo1, foo2] }),
        new NotificationDataGroup({ kind: "bar", members: [bar1, bar2] })
      ]);
    });
  });

  describe("hideOldAndAppendNew", () => {
    it("merges groups by maintaining old hidden groups, replacing old visible groups by kind, and hiding obsolete groups", () => {
      const group1 = NotificationDataGroup.groupByKind([foo1, bar1]);
      const group2 = NotificationDataGroup.groupByKind([foo2]);
      const group3 = NotificationDataGroup.groupByKind([bar2]);
      const merged1 = NotificationDataGroup.hideOldAndAppendNew(group1, group2);
      const merged2 = NotificationDataGroup.hideOldAndAppendNew(merged1, group3);
      expect(merged1).toEqual([
        new NotificationDataGroup({
          kind: "foo",
          members: [foo2],
          hidden: false
        }),
        new NotificationDataGroup({
          kind: "bar",
          members: [bar1],
          hidden: true
        })
      ]);
      expect(merged2).toEqual([
        new NotificationDataGroup({
          kind: "foo",
          members: [foo2],
          hidden: true
        }),
        new NotificationDataGroup({
          kind: "bar",
          members: [bar1],
          hidden: true
        }),
        new NotificationDataGroup({
          kind: "bar",
          members: [bar2],
          hidden: false
        })
      ]);
    });
  });
});
