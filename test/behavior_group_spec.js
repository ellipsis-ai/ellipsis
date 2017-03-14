const BehaviorGroup = require('../app/assets/javascripts/models/behavior_group');

const behaviorGroupData = Object.freeze({
  "id": "abcdef",
  "teamId": "sfgsdf",
  "name": "Some skill",
  "actionInputs": [],
  "dataTypeInputs": [],
  "behaviorVersions": [],
  "createdAt": 1468338136532
});

describe('BehaviorGroup', () => {
  describe('isRecentlySaved', () => {
    it('returns true if createdAt within the past minute', () => {
      const version = BehaviorGroup.fromJson(Object.assign({}, behaviorGroupData, { createdAt: new Date() - 50000 }));
      expect(version.isRecentlySaved()).toBe(true);
    });

    it('returns false if createdAt older than a minute ago', () => {
      const version = BehaviorGroup.fromJson(Object.assign({}, behaviorGroupData, { createdAt: new Date() - 70000 }));
      expect(version.isRecentlySaved()).toBe(false);
    });

  });

});
