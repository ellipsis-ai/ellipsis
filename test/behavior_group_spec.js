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
    const HALF_MINUTE = 30000;
    const TWO_MINUTES = 120000;
    it('returns true if createdAt within the past minute', () => {
      const version = BehaviorGroup.fromJson(Object.assign({}, behaviorGroupData, { createdAt: new Date() - HALF_MINUTE }));
      expect(version.isRecentlySaved()).toBe(true);
    });

    it('returns false if createdAt older than a minute ago', () => {
      const version = BehaviorGroup.fromJson(Object.assign({}, behaviorGroupData, { createdAt: new Date() - TWO_MINUTES }));
      expect(version.isRecentlySaved()).toBe(false);
    });

  });

});
