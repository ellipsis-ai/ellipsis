jest.unmock('../app/assets/javascripts/models/behavior_version');
jest.unmock('../app/assets/javascripts/models/param');
jest.unmock('../app/assets/javascripts/models/response_template');
jest.unmock('../app/assets/javascripts/models/trigger');

const BehaviorVersion = require('../app/assets/javascripts/models/behavior_version');

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
});
