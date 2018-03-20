import NotificationDataGroup from "../../../../app/assets/frontend/models/notification_data_group";
import UnknownParamInTemplateNotificationData from "../../../../app/assets/frontend/models/notifications/unknown_param_in_template_notification_data";
import AWSUnusedNotificationData from "../../../../app/assets/frontend/models/notifications/aws_unused_notification_data";

describe("NotificationDataGroup", () => {

  const foo1 = new UnknownParamInTemplateNotificationData({ name: "alpha" });
  const bar1 = new AWSUnusedNotificationData({ code: "bravo" });
  const foo2 = new UnknownParamInTemplateNotificationData({ name: "charlie" });
  const bar2 = new AWSUnusedNotificationData({ code: "delta" });

  describe("groupByKind", () => {
    it("assembles a list of NotificationData into an array of groups by kind", () => {
      const notifications = [foo1, bar1, foo2, bar2];
      expect(NotificationDataGroup.groupByKind(notifications)).toEqual([
        new NotificationDataGroup({ kind: "unknown_param_in_template", members: [foo1, foo2] }),
        new NotificationDataGroup({ kind: "aws_unused", members: [bar1, bar2] })
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
          kind: "unknown_param_in_template",
          members: [foo2],
          hidden: false
        }),
        new NotificationDataGroup({
          kind: "aws_unused",
          members: [bar1],
          hidden: true
        })
      ]);
      expect(merged2).toEqual([
        new NotificationDataGroup({
          kind: "unknown_param_in_template",
          members: [foo2],
          hidden: true
        }),
        new NotificationDataGroup({
          kind: "aws_unused",
          members: [bar1],
          hidden: true
        }),
        new NotificationDataGroup({
          kind: "aws_unused",
          members: [bar2],
          hidden: false
        })
      ]);
    });
  });
});
