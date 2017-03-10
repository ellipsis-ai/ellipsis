const BehaviorVersion = require('../app/assets/javascripts/models/behavior_version');
const Trigger = require('../app/assets/javascripts/models/trigger');

const behaviorVersionTask1 = Object.freeze({
  "teamId": "abcdef",
  "groupId": "sfgsdf",
  "behaviorId": "ghijkl",
  "functionBody": "use strict;",
  "responseTemplate": "A template",
  "params": [],
  "triggers": [{
    "text": "B",
    "requiresMention": false,
    "isRegex": true,
    "caseSensitive": false
  }, {
    "text": "C",
    "requiresMention": false,
    "isRegex": false,
    "caseSensitive": false
  }],
  "config": {
    "aws": {
      "accessKeyName": "AWS_ACCESS_KEY",
      "secretKeyName": "AWS_SECRET_KEY",
      "regionName": "AWS_REGION"
    }
  },
  "createdAt": 1468338136532
});
const behaviorVersionTask2 = Object.freeze({
  "teamId": "abcdef",
  "groupId": "gsdfgsg",
  "behaviorId": "mnopqr",
  "functionBody": "use strict;",
  "responseTemplate": "A template",
  "params": [],
  "triggers": [{
    "text": "A",
    "requiresMention": true,
    "isRegex": true,
    "caseSensitive": false
  }, {
    "text": ".+",
    "requiresMention": false,
    "isRegex": true,
    "caseSensitive": false
  }],
  "config": {
    "aws": {
      "accessKeyName": "AWS_ACCESS_KEY",
      "secretKeyName": "AWS_SECRET_KEY",
      "regionName": "AWS_REGION"
    }
  },
  "createdAt": 1468359271138
});
const behaviorVersionKnowledge1 = Object.freeze({
  "teamId": "abcdef",
  "groupId": "jfghjfg",
  "behaviorId": "stuvwx",
  "functionBody": "",
  "responseTemplate": "The magic 8-ball says:\n\n“Concentrate and ask again.”",
  "params": [],
  "triggers": [{
    "text": "",
    "requiresMention": false,
    "isRegex": false,
    "caseSensitive": false
  }, {
    "text": "",
    "requiresMention": false,
    "isRegex": false,
    "caseSensitive": false
  }],
  "config": {},
  "createdAt": 1466109904858
});

describe('BehaviorVersion', () => {
  describe('findFirstTriggerIndexForDisplay', () => {
    it('returns the first non-regex trigger with text when available', () => {
      const version = BehaviorVersion.fromJson(behaviorVersionTask1);
      expect(version.findFirstTriggerIndexForDisplay()).toBe(1);
    });

    it('returns the first trigger when they’re all regex', () => {
      const version = BehaviorVersion.fromJson(behaviorVersionTask2);
      expect(version.findFirstTriggerIndexForDisplay()).toBe(0);
    });

    it('returns the first trigger when none have text', () => {
      const version = BehaviorVersion.fromJson(behaviorVersionKnowledge1);
      expect(version.findFirstTriggerIndexForDisplay()).toBe(0);
    });
  });

  describe('fromJson/construction', () => {
    it('includes the defined triggers', () => {
      const data = behaviorVersionTask1;
      const version = BehaviorVersion.fromJson(data);
      expect(version.triggers.length).toEqual(2);
      expect(version.triggers.map(ea => ea.text)).toEqual(["B", "C"]);
    });

    it('includes a single blank trigger when no triggers are defined', () => {
      const data = Object.assign({}, behaviorVersionTask1);
      delete data.triggers;
      const version = BehaviorVersion.fromJson(data);
      expect(version.triggers).toEqual([new Trigger()]);
    });
  });

  describe('equality', () => {
    it('finds two versions unequal with different names', () => {
      const version1 = BehaviorVersion.fromJson(behaviorVersionTask1).clone({ name: "Thing1" });
      const version2 = version1.clone({ name: "Thing2" });
      expect(version1.isIdenticalToVersion(version2)).toBe(false);
    });
    it('finds two versions equal with different editor scroll positions and timestamps', () => {
      const version1 = BehaviorVersion.fromJson(behaviorVersionTask1).clone({ editorScrollPosition: 0, createdAt: 1450000000000 });
      const version2 = version1.clone({ editorScrollPosition: 100, createdAt: 1451000000000 });
      expect(version1.isIdenticalToVersion(version2)).toBe(true);
    });
  });

  describe('toJSON', () => {
    it('nulls out createdAt and editorScrollPosition from JSON stringify for save/export', () => {
      const version = BehaviorVersion.fromJson(behaviorVersionTask1).clone({ editorScrollPosition: 100 });
      const versionJson = JSON.stringify(version);
      const jsonObjectProps = JSON.parse(versionJson);
      expect(jsonObjectProps.createdAt).toBe(null);
      expect(jsonObjectProps.editorScrollPosition).toBe(null);
    });
  });
});
